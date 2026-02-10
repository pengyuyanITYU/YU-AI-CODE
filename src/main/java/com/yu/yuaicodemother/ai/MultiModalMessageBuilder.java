package com.yu.yuaicodemother.ai;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.yu.yuaicodemother.model.enums.FileTypeEnum;
import com.yu.yuaicodemother.model.enums.ProcessStatusEnum;
import com.yu.yuaicodemother.model.vo.file.FileProcessResult;
import dev.langchain4j.data.message.Content;
import dev.langchain4j.data.message.ImageContent;
import dev.langchain4j.data.message.TextContent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class MultiModalMessageBuilder {

    public List<Content> buildMessage(String userPrompt, List<FileProcessResult> files) {
        List<Content> contents = new ArrayList<>();
        contents.add(TextContent.from(userPrompt));

        if (files == null || files.isEmpty()) {
            return contents;
        }

        for (FileProcessResult file : files) {
            if (file == null || !ProcessStatusEnum.SUCCESS.getValue().equals(file.getStatus())) {
                continue;
            }

            // 1. 优先处理图片列表 (PDF 视觉化渲染结果)
            if (CollUtil.isNotEmpty(file.getImageBase64s())) {
                for (String imageBase64 : file.getImageBase64s()) {
                    if (StrUtil.isNotBlank(imageBase64)) {
                        contents.add(ImageContent.from(imageBase64));
                    }
                }
                log.info("Added {} visual pages for PDF: {}", file.getImageBase64s().size(), file.getFileName());
                continue;
            }

            // 2. 处理单张图片 (普通图片上传)
            if (FileTypeEnum.IMAGE.getValue().equals(file.getFileType())) {
                String imagePayload = StrUtil.isNotBlank(file.getContent()) ? file.getContent() : file.getUrl();
                if (StrUtil.isBlank(imagePayload)) {
                    log.warn("Skip image file because content and url are both empty: {}", file.getFileName());
                    continue;
                }

                contents.add(ImageContent.from(imagePayload));
                log.info("Added image content to multimodal message: {}", file.getFileName());
                continue;
            }

            // 3. 处理文本内容 (PDF 文本提取兜底，或纯文本文件)
            if (StrUtil.isNotBlank(file.getContent())) {
                String docContext = buildDocumentContext(file);
                contents.add(TextContent.from(docContext));
                log.info("Added document context to multimodal message: {}, length={}",
                        file.getFileName(), file.getContent().length());
                continue;
            }

            // 4. 处理熔断后的 URL 回退 (只有 URL，没有内容)
            if (StrUtil.isNotBlank(file.getUrl())) {
                String fallbackMsg = String.format("\n[System Note: File \"%s\" is too large to process visually/textually. Please access it via URL: %s]\n",
                        file.getFileName(), file.getUrl());
                contents.add(TextContent.from(fallbackMsg));
                log.info("Added fallback URL message for large file: {}", file.getFileName());
            }
        }

        return contents;
    }

    private String buildDocumentContext(FileProcessResult file) {
        String fileName = StrUtil.blankToDefault(file.getFileName(), "unnamed_file");
        String fileType = StrUtil.blankToDefault(file.getFileType(), "document");
        String parseMethod = getParseMethod(file.getMetadata());
        String truncatedNote = isTruncated(file.getMetadata()) ? " (content truncated)" : "";

        return String.format(
                "\n\nUser uploaded file \"%s\" (type: %s, parseMethod: %s%s). Content:\n<file_content>\n%s\n</file_content>\n",
                fileName,
                fileType,
                parseMethod,
                truncatedNote,
                file.getContent()
        );
    }

    private String getParseMethod(Map<String, Object> metadata) {
        if (metadata == null) {
            return "unknown";
        }

        Object parseMethod = metadata.get("parseMethod");
        return parseMethod == null ? "unknown" : String.valueOf(parseMethod);
    }

    private boolean isTruncated(Map<String, Object> metadata) {
        if (metadata == null) {
            return false;
        }

        Object truncated = metadata.get("truncated");
        return Boolean.TRUE.equals(truncated) || "true".equalsIgnoreCase(String.valueOf(truncated));
    }
}
