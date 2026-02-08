package com.yu.yuaicodemother.ai;

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

            if (StrUtil.isNotBlank(file.getContent())) {
                String docContext = buildDocumentContext(file);
                contents.add(TextContent.from(docContext));
                log.info("Added document context to multimodal message: {}, length={}",
                        file.getFileName(), file.getContent().length());
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
