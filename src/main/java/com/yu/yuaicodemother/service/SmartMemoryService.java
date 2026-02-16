package com.yu.yuaicodemother.service;

import com.yu.yuaicodemother.model.vo.MemoryCompressionStats;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;

/**
 * 智能记忆服务接口 - 分层记忆管理系统的核心API
 *
 * <p>系统定位：
 * 解决长对话场景下的Token超限问题，通过AI语义摘要实现80%-90%压缩率。
 * 替代传统的"固定窗口+原始消息"加载方式，采用"分层摘要+最近消息"智能加载。</p>
 *
 * <p>核心职责：
 * 1. 异步触发记忆总结(triggerSummaryIfNeeded)
 *    - 对话完成后由AppServiceImpl自动调用
 *    - 启发式判断复杂度(代码/错误关键词)，决定触发阈值
 *    - 自动生成SHORT摘要，累积后合并为MID/LONG
 *
 * 2. 智能加载记忆(loadSmartMemory)
 *    - 替代chatHistoryService.loadChatHistoryToMemory()
 *    - 按优先级加载：LONG摘要 → MID摘要 → SHORT摘要 → 最近原始消息
 *    - 兜底截断：超过70%上下文窗口时自动丢弃旧摘要
 *
 * 3. 统计监控(getCompressionStats)
 *    - 提供压缩率、节省Token数、各层摘要数量等统计
 *    - 支持系统调优和效果评估</p>
 *
 * <p>使用示例：
 * <pre>
 * // 1. 对话完成后触发(在AppServiceImpl.chatToGenCode中)
 * .doOnComplete(() -> {
 *     // ... 其他逻辑 ...
 *     smartMemoryService.triggerSummaryIfNeeded(appId, userId);
 * })
 *
 * // 2. 创建AI服务时加载记忆(在AiCodeGeneratorServiceFactory中)
 * if (smartMemoryProperties.isEnabled()) {
 *     smartMemoryService.loadSmartMemory(appId, chatMemory);
 * }
 *
 * // 3. 获取统计信息(监控或管理后台)
 * MemoryCompressionStats stats = smartMemoryService.getCompressionStats(appId);
 * </pre></p>
 *
 * @see com.yu.yuaicodemother.service.impl.SmartMemoryServiceImpl
 * @see com.yu.yuaicodemother.ai.AiCodeGeneratorServiceFactory
 * @see com.yu.yuaicodemother.service.impl.AppServiceImpl
 */
public interface SmartMemoryService {

    /**
     * 触发记忆总结(异步) - 在对话完成后自动调用
     *
     * <p>执行流程：
     * 1. 检查是否启用智能记忆(smart-memory.enabled)
     * 2. 获取上次SHORT摘要覆盖的消息ID
     * 3. 查询该ID之后的未总结消息
     * 4. 启发式分类复杂度(代码块/错误关键词)
     * 5. 达到阈值后执行：生成SHORT → 尝试合并MID → 尝试合并LONG
     * 6. 记录监控指标</p>
     *
     * <p>触发时机：
     * - 在AppServiceImpl.chatToGenCode的doOnComplete回调中调用
     * - 使用@Async异步执行，不阻塞用户响应
     * - 异常被捕获，不影响主流程</p>
     *
     * @param appId 应用ID，标识哪个应用的对话
     * @param userId 用户ID，用于记录摘要创建者
     */
    void triggerSummaryIfNeeded(Long appId, Long userId);

    /**
     * 智能加载记忆到ChatMemory - 替代传统的原始消息加载
     *
     * <p>加载策略：
     * 1. 如果智能记忆未启用，回退到chatHistoryService.loadChatHistoryToMemory(20条)
     * 2. 清空现有记忆：chatMemory.clear()
     * 3. 构建摘要上下文：LONG + MID + SHORT(按优先级拼接)
     * 4. 将摘要作为SystemMessage注入
     * 5. 加载最近N条原始消息(默认10条)
     * 6. 兜底截断：总Token超过70%上下文窗口时，丢弃旧SHORT摘要</p>
     *
     * <p>优势对比：
     * - 传统方式：加载20条原始消息 ≈ 4000-8000 Token
     * - 智能方式：摘要+最近消息 ≈ 800-1500 Token
     * - 压缩率：80%-90%，同时保留关键信息</p>
     *
     * @param appId 应用ID
     * @param chatMemory LangChain4j的记忆容器，AI模型通过它获取历史上下文
     */
    void loadSmartMemory(Long appId, MessageWindowChatMemory chatMemory);

    /**
     * 获取记忆压缩统计 - 用于监控和评估效果
     *
     * <p>统计指标：
     * - totalOriginalTokens: 所有被摘要消息的原始Token总数
     * - totalSummaryTokens: 所有摘要的Token总数
     * - compressionRatio: 压缩率 (1 - 摘要/原始)
     * - short/mid/longSummaryCount: 各层摘要数量
     * - unsummarizedMessageCount: 尚未被总结的消息数</p>
     *
     * <p>使用场景：
     * - 管理后台展示应用的记忆统计
     * - Prometheus监控上报
     * - 系统效果评估和调优依据</p>
     *
     * @param appId 应用ID
     * @return 压缩统计对象，包含Token数、压缩率、摘要数量等信息
     */
    MemoryCompressionStats getCompressionStats(Long appId);
}
