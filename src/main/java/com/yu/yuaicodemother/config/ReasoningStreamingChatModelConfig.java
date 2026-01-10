package com.yu.yuaicodemother.config;

import com.yu.yuaicodemother.monitor.AiModelMonitorListener;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import jakarta.annotation.Resource;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@ConfigurationProperties(prefix = "langchain4j.open-ai.reasoning-chat-model")
@Data
public class ReasoningStreamingChatModelConfig {

    private String baseUrl;

    private String apiKey;

    private String modelName;

    private Integer maxTokens;

    private boolean logRequests;

    private boolean logResponses;

    @Resource
    private AiModelMonitorListener aiModelMonitorListener;

    /**
     * 推理流式模型（用于 Vue 项目生成，带工具调用）
     */
    @Bean
    public StreamingChatModel reasoningStreamingChatModel() {
//        // 皮  为了测试方便临时修改
//        final String modelName = "deepseek-chat";
//        final int maxTokens = 8192;
//        // 生产环境使用：
//        // final String modelName = "deepseek-reasoner";
//        // final int maxTokens = 32768;
        return OpenAiStreamingChatModel.builder()
                .apiKey(apiKey)
                .baseUrl(baseUrl)
                .modelName(modelName)
                .maxTokens(maxTokens)
                .listeners(List.of(aiModelMonitorListener))
                .logRequests(logRequests)
                .logResponses(logResponses)
                .build();
    }
}
