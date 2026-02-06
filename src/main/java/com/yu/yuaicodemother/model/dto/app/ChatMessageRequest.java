package com.yu.yuaicodemother.model.dto.app;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

@Data
public class ChatMessageRequest implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @NotNull(message = "应用ID不能为空")
    private Long appId;

    @NotBlank(message = "消息内容不能为空")
    private String message;

    private List<FileAttachment> files;

    @Data
    public static class FileAttachment implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;

        private String url;
        private String fileName;
        private String fileType;
        private String content;
    }
}
