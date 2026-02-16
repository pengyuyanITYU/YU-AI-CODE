package com.yu.yuaicodemother.config;

import com.yu.yuaicodemother.ai.MemorySummarizeAiService;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.allminilml6v2.AllMiniLmL6V2EmbeddingModel;
import dev.langchain4j.service.AiServices;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

/**
 * 智能记忆配置类 - Spring配置和Bean定义
 *
 * <p>配置说明：
 * 1. EmbeddingModel: 本地AllMiniLmL6V2嵌入模型
 *    - 延迟初始化(@Lazy)，首次使用时加载，约30MB内存
 *    - 用于计算摘要语义向量，实现相似度去重
 *
 * 2. MemorySummarizeAiService: AI摘要服务代理
 *    - 基于LangChain4j的AiServices构建
 *    - 注入ChatModel和Prompt配置
 *    - 延迟初始化，避免启动时连接AI服务</p>
 *
 * <p>注意事项：
 * - 使用@Lazy避免循环依赖
 * - Embedding模型首次调用时加载，耗时2-5秒
 * - 生产环境建议预热</p>
 *
 * @see com.yu.yuaicodemother.ai.MemorySummarizeAiService
 * @see dev.langchain4j.model.embedding.EmbeddingModel
 */
@Slf4j
@Configuration
public class SmartMemoryConfig {

    /**
     * 创建本地Embedding模型Bean - 用于语义相似度计算
     *
     * <p>使用AllMiniLmL6V2模型，约30MB内存，首次调用时加载。</p>
     *
     * @return Embedding模型实例
     */
    @Bean
    @Lazy
    public EmbeddingModel embeddingModel() {
        log.info("初始化本地 Embedding 模型 (AllMiniLmL6V2)");
        return new AllMiniLmL6V2EmbeddingModel();
    }

    /**
     * 创建AI摘要服务代理Bean - 用于生成三层摘要
     *
     * <p>基于LangChain4j的AiServices构建，注入ChatModel和Prompt配置。</p>
     *
     * @param generateAppNameChatModel 聊天模型
     * @return AI摘要服务代理实例
     */
    @Bean
    @Lazy
    public MemorySummarizeAiService memorySummarizeAiService(ChatModel generateAppNameChatModel) {
        log.info("[智能记忆] 创建AI摘要服务代理");
        return AiServices.builder(MemorySummarizeAiService.class)
                .chatModel(generateAppNameChatModel)
                .build();
    }
}
