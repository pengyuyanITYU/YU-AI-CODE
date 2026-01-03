package com.yu.yuaicodemother.ai.model.message;

import com.mybatisflex.annotation.KeyType;
import com.yu.yuaicodemother.model.enums.StreamMessageTypeEnum;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class DeepThinkMessage extends StreamMessage{

    private String id;

    /**
     * 会话 ID (用于关联上下文)
     */
    private String sessionId;

    /**
     * 用户的问题
     */
    private String userQuery;

    /**
     * AI 的最终回答 (Markdown 格式)
     * 对应 LangChain4j AiMessage.text()
     */
    private String content;

    /**
     * 【核心字段】AI 的思考过程 / 推理链
     * 对应 DeepSeek 的 reasoning_content
     * 建议数据库类型设为 LONGTEXT
     */
    private String reasoningContent;

    public DeepThinkMessage(String id, String sessionId, String userQuery, String content, String reasoningContent) {
        super(StreamMessageTypeEnum.Deep_THINK.getValue());
        this.id = id;
        this.sessionId = sessionId;
        this.userQuery = userQuery;
    }
}
