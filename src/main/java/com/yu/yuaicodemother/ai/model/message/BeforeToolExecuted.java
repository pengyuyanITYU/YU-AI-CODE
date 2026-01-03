package com.yu.yuaicodemother.ai.model.message;

import com.yu.yuaicodemother.model.enums.StreamMessageTypeEnum;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * 工具调用消息
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class BeforeToolExecuted extends StreamMessage {

    private String id;

    private String name;

    private String arguments;

    public BeforeToolExecuted(ToolExecutionRequest toolExecutionRequest) {
        super(StreamMessageTypeEnum.TOOL_REQUEST.getValue());
        this.id = toolExecutionRequest.id();
        this.name = toolExecutionRequest.name();
        this.arguments = toolExecutionRequest.arguments();
    }
}
