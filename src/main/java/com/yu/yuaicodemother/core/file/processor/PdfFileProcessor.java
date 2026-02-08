package com.yu.yuaicodemother.core.file.processor;

import cn.hutool.core.util.StrUtil;
import com.yu.yuaicodemother.manager.TencentOcrManager;
import com.yu.yuaicodemother.model.enums.FileTypeEnum;
import com.yu.yuaicodemother.model.enums.ProcessStatusEnum;
import com.yu.yuaicodemother.model.vo.file.FileProcessResult;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * PDF 文件处理器
 * 采用 PDFBox 文本提取为主，腾讯云 OCR 识别为辅的策略
 */
@Component
@Slf4j
public class PdfFileProcessor implements FileContentProcessor {

    private static final int MAX_CHARS = 30000; // 最大提取字符数
    private static final int MIN_TEXT_LENGTH = 50; // 触发 OCR 的最小文本长度阈值
    private static final String TRUNCATION_NOTE = "[System Note: PDF content truncated due to length limit.]"; // 截断提示

    @Resource
    private TencentOcrManager tencentOcrManager;

    @Override
    public FileProcessResult process(File file, String fileUrl) {
        PdfTextExtractResult textExtractResult = extractTextWithPdfBox(file);

        String text = textExtractResult.text();
        // 判断 PDFBox 提取的内容是否足够（防止扫描件提取出乱码或空内容）
        boolean hasSufficientPdfText = StrUtil.isNotBlank(text) && text.trim().length() >= MIN_TEXT_LENGTH;
        boolean ocrUsed = false;
        String parseMethod = hasSufficientPdfText ? "pdfbox" : "ocr";

        if (!hasSufficientPdfText) {
            // 如果文本提取不足，尝试使用 OCR 识别
            try {
                log.info("PDF text insufficient, starting OCR: {}", file.getName());
                text = tencentOcrManager.recognizePdf(file);
                ocrUsed = true;
                if (StrUtil.isNotBlank(textExtractResult.text())) {
                    parseMethod = "pdfbox+ocr";
                }
            } catch (Exception ocrError) {
                log.error("OCR failed while processing PDF: {}", file.getName(), ocrError);
                return buildResult(
                        file,
                        fileUrl,
                        null,
                        ProcessStatusEnum.FAILED,
                        "PDF parsing failed and OCR fallback also failed: " + ocrError.getMessage(),
                        buildMetadata(file, textExtractResult.pageCount(), "ocr-failed", 0, false, true)
                );
            }
        }

        if (StrUtil.isBlank(text)) {
            return buildResult(
                    file,
                    fileUrl,
                    null,
                    ProcessStatusEnum.EMPTY,
                    "Unable to extract content from PDF",
                    buildMetadata(file, textExtractResult.pageCount(), parseMethod, 0, false, ocrUsed)
            );
        }

        // 文本标准化与长度截断
        DocumentTextNormalizer.NormalizeResult normalizedResult =
                DocumentTextNormalizer.normalizeAndLimit(text, MAX_CHARS, TRUNCATION_NOTE);

        Map<String, Object> metadata = buildMetadata(
                file,
                textExtractResult.pageCount(),
                parseMethod,
                normalizedResult.originalLength(),
                normalizedResult.truncated(),
                ocrUsed
        );

        log.info("PDF processed successfully: {}, parseMethod={}, pageCount={}, length={}",
                file.getName(), parseMethod, textExtractResult.pageCount(), normalizedResult.text().length());

        return buildResult(
                file,
                fileUrl,
                normalizedResult.text(),
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
     * 使用 PDFBox 提取文本
     *
     * @param file PDF文件
     * @return 提取结果（文本内容及页数）
     */
    private PdfTextExtractResult extractTextWithPdfBox(File file) {
        try (PDDocument document = Loader.loadPDF(file)) {
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(document);
            return new PdfTextExtractResult(text, document.getNumberOfPages());
        } catch (IOException e) {
            log.warn("PDFBox extraction failed, fallback to OCR for file: {}", file.getName(), e);
            return new PdfTextExtractResult("", 0);
        }
    }

    private FileProcessResult buildResult(File file,
                                          String url,
                                          String content,
                                          ProcessStatusEnum status,
                                          String errorMsg,
                                          Map<String, Object> metadata) {
        return FileProcessResult.builder()
                .fileType(FileTypeEnum.DOCUMENT.getValue())
                .url(url)
                .content(content)
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
