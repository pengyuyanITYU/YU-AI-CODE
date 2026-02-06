package com.yu.yuaicodemother.core.file.processor;

import com.yu.yuaicodemother.manager.TencentOcrManager;
import com.yu.yuaicodemother.model.enums.FileTypeEnum;
import com.yu.yuaicodemother.model.enums.ProcessStatusEnum;
import com.yu.yuaicodemother.model.vo.file.FileProcessResult;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

@Component
@Slf4j
public class PdfFileProcessor implements FileContentProcessor {

    @Resource
    private TencentOcrManager tencentOcrManager;

    private final Tika tika = new Tika();

    @Override
    public FileProcessResult process(File file, String fileUrl) {
        try {
            String text = tika.parseToString(file);

            if (text == null || text.trim().isEmpty()) {
                if (file.length() > 0) {
                    log.warn("PDF可能是扫描版，尝试OCR识别: {}", file.getName());
                    try {
                        text = tencentOcrManager.recognizePdf(file);
                    } catch (Exception ocrError) {
                        log.error("OCR识别失败: {}", file.getName(), ocrError);
                        return FileProcessResult.builder()
                                .fileType(FileTypeEnum.DOCUMENT.getValue())
                                .url(fileUrl)
                                .status(ProcessStatusEnum.FAILED.getValue())
                                .errorMessage("无法识别扫描版PDF，请上传包含文字的PDF或使用OCR工具预处理")
                                .build();
                    }
                } else {
                    return FileProcessResult.builder()
                            .fileType(FileTypeEnum.DOCUMENT.getValue())
                            .url(fileUrl)
                            .status(ProcessStatusEnum.EMPTY.getValue())
                            .errorMessage("PDF文件为空")
                            .build();
                }
            }

            text = cleanText(text);

            Map<String, Object> metadata = new HashMap<>();
            metadata.put("sizeKB", file.length() / 1024);

            log.info("PDF处理成功: {}, 内容长度: {}", file.getName(), text.length());

            return FileProcessResult.builder()
                    .fileType(FileTypeEnum.DOCUMENT.getValue())
                    .url(fileUrl)
                    .content(text)
                    .status(ProcessStatusEnum.SUCCESS.getValue())
                    .metadata(metadata)
                    .build();

        } catch (Exception e) {
            log.error("PDF处理失败: {}", file.getName(), e);
            return FileProcessResult.builder()
                    .fileType(FileTypeEnum.DOCUMENT.getValue())
                    .url(fileUrl)
                    .status(ProcessStatusEnum.FAILED.getValue())
                    .errorMessage("PDF处理失败: " + e.getMessage())
                    .build();
        }
    }

    @Override
    public boolean support(String extension) {
        return "pdf".equalsIgnoreCase(extension);
    }

    private String cleanText(String text) {
        if (text == null) {
            return "";
        }
        return text.replaceAll("\\s+", " ").trim();
    }
}
