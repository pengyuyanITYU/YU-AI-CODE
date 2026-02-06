package com.yu.yuaicodemother.ai;

import com.yu.yuaicodemother.model.enums.FileTypeEnum;
import com.yu.yuaicodemother.model.vo.file.FileProcessResult;
import dev.langchain4j.data.message.Content;
import dev.langchain4j.data.message.ImageContent;
import dev.langchain4j.data.message.TextContent;
import dev.langchain4j.data.message.UserMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
public class MultiModalMessageBuilder {

    public UserMessage buildMessage(String userPrompt, List<FileProcessResult> files) {
        if (files == null || files.isEmpty()) {
            return UserMessage.from(userPrompt);
        }

        List<Content> contents = new ArrayList<>();

        contents.add(TextContent.from(userPrompt));

        for (FileProcessResult file : files) {
            if (FileTypeEnum.IMAGE.getValue().equals(file.getFileType())) {
                contents.add(ImageContent.from(file.getUrl()));
                log.info("添加图片到消息: {}", file.getFileName());

            } else if (file.getContent() != null && !file.getContent().isEmpty()) {
                String docContext = buildDocumentContext(file);
                contents.add(TextContent.from(docContext));
                log.info("添加文档到消息: {}, 内容长度: {}",
                        file.getFileName(), file.getContent().length());
            }
        }

        return UserMessage.from(contents);
    }

    private String buildDocumentContext(FileProcessResult file) {
        return String.format(
                "\n\n用户上传了文档《%s》，内容如下:\n<file_content>\n%s\n</file_content>\n",
                file.getFileName(),
                file.getContent()
        );
    }
}
