package com.yu.yuaicodemother.core.file.processor;

import cn.hutool.core.codec.Base64;
import cn.hutool.core.util.StrUtil;
import com.yu.yuaicodemother.manager.TencentOcrManager;
import com.yu.yuaicodemother.model.enums.FileTypeEnum;
import com.yu.yuaicodemother.model.enums.ProcessStatusEnum;
import com.yu.yuaicodemother.model.vo.file.FileProcessResult;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * PDF 文件处理器
 * 采用 视觉化渲染 (Visual PDF) 策略：将 PDF 页面转为图片，保留版面信息
 * 包含 熔断保护机制：大文件跳过渲染
 */
@Component
@Slf4j
public class PdfFileProcessor implements FileContentProcessor {

    private static final int MAX_CHARS = 30000; // 最大提取字符数
    private static final int MIN_TEXT_LENGTH = 50; // 触发 OCR 的最小文本长度阈值
    private static final String TRUNCATION_NOTE = "[System Note: PDF content truncated due to length limit.]"; // 截断提示
    private static final long MAX_FILE_SIZE_BYTES = 10 * 1024 * 1024; // 10MB
    private static final int MAX_RENDER_PAGES = 5; // 最多渲染前5页
    private static final int RENDER_DPI = 144; // 渲染DPI
    private static final int MAX_IMAGE_DIMENSION = 1024; // 图片长边限制
    private static final float IMAGE_QUALITY = 0.8f; // 图片压缩质量

    @Resource
    private TencentOcrManager tencentOcrManager;

    @Override
    public FileProcessResult process(File file, String fileUrl) {
        // 1. 熔断保护：文件过大直接返回 URL
        if (file.length() > MAX_FILE_SIZE_BYTES) {
            log.warn("PDF file too large ({} bytes), skipping content extraction: {}", file.length(), file.getName());
            return buildResult(
                    file,
                    fileUrl,
                    null,
                    null,
                    ProcessStatusEnum.SUCCESS, // 视为成功，只是无内容
                    null,
                    buildMetadata(file, 0, "size-limit-exceeded", 0, false, false)
            );
        }

        // 2. 尝试视觉化渲染 (Render PDF to Images)
        List<String> imageBase64s = new ArrayList<>();
        int pageCount = 0;
        try {
            imageBase64s = renderPdfToImages(file);
            // 简单的页数估算，实际准确页数需从 render 过程获取或重新 open
            pageCount = imageBase64s.size(); 
        } catch (Exception e) {
            log.error("Failed to render PDF to images: {}", file.getName(), e);
            // 渲染失败不中断流程，尝试文本提取作为兜底
        }

        // 3. 文本提取 (作为元数据或补充，或者如果渲染失败时的主要内容)
        // 注意：如果已经渲染了图片，文本提取主要用于 NLP 分析或 RAG，但在当前多模态上下文中，图片是首选。
        // 为了保持兼容性和元数据完整性，我们仍然提取文本。
        PdfTextExtractResult textExtractResult = extractTextWithPdfBox(file);
        pageCount = Math.max(pageCount, textExtractResult.pageCount());

        String text = textExtractResult.text();
        boolean hasSufficientPdfText = StrUtil.isNotBlank(text) && text.trim().length() >= MIN_TEXT_LENGTH;
        boolean ocrUsed = false;
        String parseMethod = "visual"; // 默认为视觉模式

        // 如果没有渲染出图片，且文本不足，尝试 OCR (原有逻辑)
        if (imageBase64s.isEmpty() && !hasSufficientPdfText) {
             try {
                log.info("PDF visual render failed and text insufficient, starting OCR: {}", file.getName());
                text = tencentOcrManager.recognizePdf(file);
                ocrUsed = true;
                parseMethod = "ocr";
            } catch (Exception ocrError) {
                log.error("OCR failed while processing PDF: {}", file.getName(), ocrError);
                // 如果图片也没有，文本也没有，OCR 也挂了
                 if (imageBase64s.isEmpty()) {
                     return buildResult(
                             file,
                             fileUrl,
                             null,
                             null,
                             ProcessStatusEnum.FAILED,
                             "PDF parsing failed (visual/text/ocr): " + ocrError.getMessage(),
                             buildMetadata(file, textExtractResult.pageCount(), "failed", 0, false, true)
                     );
                 }
            }
        } else if (!imageBase64s.isEmpty()) {
            parseMethod = "visual+text";
        } else {
            parseMethod = "text-only";
        }

        // 文本标准化与长度截断
        DocumentTextNormalizer.NormalizeResult normalizedResult =
                DocumentTextNormalizer.normalizeAndLimit(text, MAX_CHARS, TRUNCATION_NOTE);

        Map<String, Object> metadata = buildMetadata(
                file,
                pageCount,
                parseMethod,
                normalizedResult.originalLength(),
                normalizedResult.truncated(),
                ocrUsed
        );

        log.info("PDF processed successfully: {}, parseMethod={}, pageCount={}, images={}",
                file.getName(), parseMethod, pageCount, imageBase64s.size());

        return buildResult(
                file,
                fileUrl,
                normalizedResult.text(), // 仍然保留文本内容
                imageBase64s,            // 新增图片列表
                ProcessStatusEnum.SUCCESS,
                null,
                metadata
        );
    }

    @Override
    public boolean support(String extension) {
        return "pdf".equalsIgnoreCase(extension);
    }

    /**
     * 将 PDF 页面渲染为图片并转为 Base64
     */
    private List<String> renderPdfToImages(File file) throws IOException {
        List<String> images = new ArrayList<>();
        try (PDDocument document = Loader.loadPDF(file)) {
            PDFRenderer renderer = new PDFRenderer(document);
            int pagesToRender = Math.min(document.getNumberOfPages(), MAX_RENDER_PAGES);

            for (int i = 0; i < pagesToRender; i++) {
                // 渲染页面
                BufferedImage image = renderer.renderImageWithDPI(i, RENDER_DPI, ImageType.RGB);

                // 压缩和转换
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                Thumbnails.of(image)
                        .size(MAX_IMAGE_DIMENSION, MAX_IMAGE_DIMENSION)
                        .outputQuality(IMAGE_QUALITY)
                        .outputFormat("jpg")
                        .toOutputStream(outputStream);

                String base64Data = Base64.encode(outputStream.toByteArray());
                // 拼接 Data URI Scheme
                images.add("data:image/jpeg;base64," + base64Data);
            }
        }
        return images;
    }

    /**
     * 使用 PDFBox 提取文本
     */
    private PdfTextExtractResult extractTextWithPdfBox(File file) {
        try (PDDocument document = Loader.loadPDF(file)) {
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(document);
            return new PdfTextExtractResult(text, document.getNumberOfPages());
        } catch (IOException e) {
            log.warn("PDFBox extraction failed: {}", file.getName(), e);
            return new PdfTextExtractResult("", 0);
        }
    }

    private FileProcessResult buildResult(File file,
                                          String url,
                                          String content,
                                          List<String> imageBase64s,
                                          ProcessStatusEnum status,
                                          String errorMsg,
                                          Map<String, Object> metadata) {
        return FileProcessResult.builder()
                .fileType(FileTypeEnum.DOCUMENT.getValue())
                .url(url)
                .content(content)
                .imageBase64s(imageBase64s)
                .status(status.getValue())
                .errorMessage(errorMsg)
                .metadata(metadata)
                .build();
    }

    private Map<String, Object> buildMetadata(File file,
                                              int pageCount,
                                              String parseMethod,
                                              int charCount,
                                              boolean truncated,
                                              boolean ocrUsed) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("pageCount", pageCount);
        metadata.put("parseMethod", parseMethod);
        metadata.put("charCount", charCount);
        metadata.put("truncated", truncated);
        metadata.put("ocrUsed", ocrUsed);
        metadata.put("fileSizeKB", file.length() / 1024);
        return metadata;
    }

    private record PdfTextExtractResult(String text, int pageCount) {
    }
}
