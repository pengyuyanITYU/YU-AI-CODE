package com.yu.yuaicodemother.ai;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

/**
 * AI记忆摘要服务接口 - 使用LangChain4j实现三层摘要生成
 *
 * <p>分层Prompt策略：
 * 1. summarizeShort(): 使用 prompt/memory-summarize-short.txt
 *    - 焦点：事实记录，保留代码、变量名、配置值、错误堆栈
 *    - 输入：原始对话历史(多条消息)
 *    - 输出：简洁的要点列表
 *
 * 2. summarizeMid(): 使用 prompt/memory-summarize-mid.txt
 *    - 焦点：技术决策，保留"选了什么"和"为什么选"
 *    - 输入：多个SHORT摘要拼接
 *    - 输出：按模块分组的技术决策记录
 *
 * 3. summarizeLong(): 使用 prompt/memory-summarize-long.txt
 *    - 焦点：项目目标，保留架构演进方向
 *    - 输入：多个MID摘要拼接
 *    - 输出：项目级知识摘要(目标/架构/约束)</p>
 *
 * <p>配置方式：
 * 使用LangChain4j的AiServices自动构建，通过@SystemMessage注解注入Prompt。
 * Prompt文件位于 resources/prompt/ 目录。</p>
 *
 * @see com.yu.yuaicodemother.config.SmartMemoryConfig
 * @see com.yu.yuaicodemother.service.impl.SmartMemoryServiceImpl
 */
public interface MemorySummarizeAiService {

    @SystemMessage(fromResource = "prompt/memory-summarize-short.txt")
    String summarizeShort(@UserMessage String conversationText);

    @SystemMessage(fromResource = "prompt/memory-summarize-mid.txt")
    String summarizeMid(@UserMessage String summariesText);

    @SystemMessage(fromResource = "prompt/memory-summarize-long.txt")
    String summarizeLong(@UserMessage String summariesText);
}
