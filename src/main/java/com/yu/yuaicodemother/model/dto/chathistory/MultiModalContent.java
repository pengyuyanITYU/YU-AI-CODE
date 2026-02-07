package com.yu.yuaicodemother.model.dto.chathistory;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

/**
 * 标准化多模态消息持久化模型
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class MultiModalContent {
    /**
     * 用户输入的原始文本或拼接后的提示词
     */
    private String text;

    /**
     * 附件列表
     */
    private List<AttachmentInfo> attachments;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class AttachmentInfo {
        private String fileId;
        private String fileName;
        /**
         * IMAGE / DOCUMENT
         */
        private String type;
        private String url;
        /**
         * 文档提取的纯文本内容（可选，用于 AI 记忆还原）
         */
        private String content;
    }
}
