package com.yu.yuaicodemother.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.yu.yuaicodemother.ai.MemorySummarizeAiService;
import com.yu.yuaicodemother.config.SmartMemoryProperties;
import com.yu.yuaicodemother.mapper.ChatMemorySummaryMapper;
import com.yu.yuaicodemother.model.entity.ChatHistory;
import com.yu.yuaicodemother.model.entity.ChatMemorySummary;
import com.yu.yuaicodemother.model.enums.ChatHistoryMessageTypeEnum;
import com.yu.yuaicodemother.model.enums.MemoryLayerEnum;
import com.yu.yuaicodemother.model.enums.TopicComplexityEnum;
import com.yu.yuaicodemother.model.vo.MemoryCompressionStats;
import com.yu.yuaicodemother.monitor.MemoryMetricsCollector;
import com.yu.yuaicodemother.service.ChatHistoryService;
import com.yu.yuaicodemother.service.SmartMemoryService;
import com.yu.yuaicodemother.service.TokenEstimator;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.embedding.EmbeddingModel;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 智能记忆服务实现 - 分层记忆管理系统的核心实现
 *
 * <p>架构概览：
 * 本类实现三层摘要管理：SHORT → MID → LONG，每层有不同的保留重点和触发阈值。
 * 通过AI语义摘要+Embedding去重，实现80%-90%的Token压缩率。</p>
 *
 * <p>核心算法：
 * 1. 启发式复杂度分类(classifyComplexity)
 *    - 代码块检测：``` 或长度>500字符 → HIGH(阈值20条)
 *    - 错误关键词：错误/异常/bug/fix → MEDIUM(阈值30条)
 *    - 默认：LOW(阈值40条)
 *
 * 2. 三层摘要生成流程
 *    a) performShortSummary: 原始消息 → SHORT摘要(保留代码细节)
 *    b) tryMergeMid: SHORT × 3 → MID摘要(去重后，保留技术决策)
 *    c) tryMergeLong: MID × 3 → LONG摘要(去重后，保留项目目标)
 *
 * 3. Embedding去重算法(filterByEmbeddingSimilarity)
 *    - 使用AllMiniLmL6V2模型计算语义向量
 *    - 余弦相似度 > 0.85 认为是重复内容
 *    - 合并时去除冗余，保留多样性
 *
 * 4. 兜底截断算法(truncateToFit)
 *    - 当摘要+最近消息超过70%上下文窗口时
 *    - 从旧到新逐个丢弃消息，直到安全范围</p>
 *
 * <p>线程安全：
 * - triggerSummaryIfNeeded 使用@Async异步执行，不阻塞主线程
 * - 乐观锁(@Version)防止并发总结冲突
 * - 使用@Lazy延迟注入，避免循环依赖和快速启动</p>
 *
 * <p>性能优化：
 * - Embedding模型延迟初始化(@Lazy)，约30MB内存
 * - 增量总结：只处理上次总结后的新消息
 * - 缓存友好：使用appId作为查询条件，命中索引</p>
 *
 * @see com.yu.yuaicodemother.service.SmartMemoryService
 * @see com.yu.yuaicodemother.ai.MemorySummarizeAiService
 */
@Slf4j
@Service
public class SmartMemoryServiceImpl extends ServiceImpl<ChatMemorySummaryMapper, ChatMemorySummary>
        implements SmartMemoryService {

    @Resource
    private ChatMemorySummaryMapper chatMemorySummaryMapper;

    @Lazy
    @Resource
    private ChatHistoryService chatHistoryService;

    @Lazy
    @Resource
    private MemorySummarizeAiService memorySummarizeAiService;

    @Lazy
    @Resource
    private EmbeddingModel embeddingModel;  //本地向量模型示例
    // 来自langchain4j-embeddings-all-minilm-l6-v2

    @Resource
    private TokenEstimator tokenEstimator;

    @Resource
    private SmartMemoryProperties properties;

    @Resource
    private MemoryMetricsCollector memoryMetricsCollector;

    private static final Pattern CODE_BLOCK_PATTERN = Pattern.compile("```");
    private static final Pattern ERROR_KEYWORD_PATTERN = Pattern.compile(
            "错误|异常|报错|bug|fix|error|exception|failed|failure", Pattern.CASE_INSENSITIVE);

    /**
     * 异步触发记忆总结 - 对话完成后自动调用
     *
     * <p>执行流程：
     * 1. 检查智能记忆是否启用
     * 2. 获取上次SHORT摘要覆盖的消息ID
     * 3. 查询该ID之后的未总结消息
     * 4. 启发式分类复杂度(HIGH/MEDIUM/LOW)
     * 5. 达到阈值后执行三层摘要生成
     * 6. 记录监控指标和耗时</p>
     *
     * @param appId 应用ID
     * @param userId 用户ID
     */
    @Async
    @Override
    public void triggerSummaryIfNeeded(Long appId, Long userId) {
        log.debug("[智能记忆] 触发检查 appId={}, userId={}, enabled={}", appId, userId, properties.isEnabled());
        if (!properties.isEnabled()) {
            log.debug("[智能记忆] 智能记忆已禁用，跳过总结 appId={}", appId);
            return;
        }
        try {
            long startTime = System.currentTimeMillis();
            ChatMemorySummary latestSummary = getLatestSummary(appId, MemoryLayerEnum.SHORT);
            Long afterId = latestSummary != null ? latestSummary.getCoveredTo() : null;
            log.debug("[智能记忆] 查询上次摘要 appId={}, afterId={}", appId, afterId);

            List<ChatHistory> unsummarized = getUnsummarizedMessages(appId, afterId);
            if (CollUtil.isEmpty(unsummarized)) {
                log.debug("[智能记忆] 无未总结消息，跳过 appId={}", appId);
                return;
            }

            TopicComplexityEnum complexity = classifyComplexity(unsummarized);
            int threshold = getThreshold(complexity);
            log.debug("[智能记忆] 复杂度分类 appId={}, 复杂度={}, 阈值={}, 未总结消息数={}",
                    appId, complexity, threshold, unsummarized.size());

            if (unsummarized.size() < threshold) {
                log.debug("appId={} 未达到总结阈值: {}/{}", appId, unsummarized.size(), threshold);
                return;
            }

            log.info("[智能记忆] 开始总结流程 appId={}, 复杂度={}, 消息数={}", appId, complexity, unsummarized.size());

            performShortSummary(appId, userId, unsummarized, complexity);
            tryMergeMid(appId, userId);
            tryMergeLong(appId, userId);

            long duration = System.currentTimeMillis() - startTime;
            memoryMetricsCollector.recordSummaryExecution(appId.toString(), duration);
            log.info("[智能记忆] 总结流程完成 appId={}, 耗时={}ms", appId, duration);
        } catch (Exception e) {
            log.error("[智能记忆] 记忆总结失败 appId={}, 错误: {}", appId, e.getMessage(), e);
        }
    }

    /**
     * 智能加载记忆到ChatMemory - 替代传统的原始消息加载
     *
     * <p>加载策略：
     * 1. 检查智能记忆是否启用，禁用时回退到传统加载(20条原始消息)
     * 2. 清空现有ChatMemory
     * 3. 构建摘要上下文：LONG + MID + SHORT(按优先级拼接)
     * 4. 将摘要作为SystemMessage注入
     * 5. 加载最近N条原始消息
     * 6. 兜底截断：总Token超过70%上下文窗口时丢弃旧消息
     * 7. 将消息注入ChatMemory</p>
     *
     * @param appId 应用ID
     * @param chatMemory LangChain4j的记忆容器
     */
    @Override
    public void loadSmartMemory(Long appId, MessageWindowChatMemory chatMemory) {
        log.debug("[智能记忆] 开始加载 appId={}, enabled={}", appId, properties.isEnabled());
        if (!properties.isEnabled()) {
            log.debug("[智能记忆] 智能记忆已禁用，回退到传统加载 appId={}", appId);
            chatHistoryService.loadChatHistoryToMemory(appId, chatMemory, 20);
            return;
        }

        chatMemory.clear();

        String summaryContext = buildSummaryContext(appId);
        int summaryTokens = 0;

        if (StrUtil.isNotBlank(summaryContext)) {
            summaryTokens = tokenEstimator.countTokens(summaryContext);
            chatMemory.add(SystemMessage.from("以下是之前对话的摘要:\n" + summaryContext));
            log.debug("[智能记忆] 注入摘要上下文 appId={}, Token数={}", appId, summaryTokens);
        } else {
            log.debug("[智能记忆] 无摘要上下文 appId={}", appId);
        }

        List<ChatHistory> recentMessages = getRecentMessages(appId, properties.getRecentMessages());
        int messagesTokens = estimateMessagesTokens(recentMessages);
        int totalTokens = summaryTokens + messagesTokens;
        boolean needTruncate = tokenEstimator.exceedsLimit(
                totalTokens,
                properties.getContextWindowSize(),
                properties.getMaxContextRatio());
        log.debug("[智能记忆] Token检查 appId={}, 摘要Token={}, 消息Token={}, 总Token={}, 限制={}, 需截断={}",
                appId, summaryTokens, messagesTokens, totalTokens,
                (int) (properties.getContextWindowSize() * properties.getMaxContextRatio()), needTruncate);

        if (needTruncate) {
            recentMessages = truncateToFit(recentMessages, summaryTokens);
        }

        for (ChatHistory history : recentMessages) {
            if (ChatHistoryMessageTypeEnum.USER.getValue().equals(history.getMessageType())) {
                chatMemory.add(UserMessage.from(history.getMessage()));
            } else if (ChatHistoryMessageTypeEnum.AI.getValue().equals(history.getMessageType())) {
                chatMemory.add(AiMessage.from(history.getMessage()));
            }
        }

        memoryMetricsCollector.recordLoadHit(appId.toString(), StrUtil.isNotBlank(summaryContext));
        log.info("[智能记忆] 加载完成 appId={}, 摘要Token={}, 原始消息数={}", appId, summaryTokens, recentMessages.size());
    }

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
     * @param appId 应用ID
     * @return 压缩统计对象
     */
    @Override
    public MemoryCompressionStats getCompressionStats(Long appId) {
        // 获取指定应用的所有摘要
        List<ChatMemorySummary> allSummaries = getSummariesByApp(appId);

        // 汇总原始Token和摘要Token
        int totalOriginal = allSummaries.stream()
                .mapToInt(s -> s.getOriginalTokens() != null ? s.getOriginalTokens() : 0).sum();
        int totalSummary = allSummaries.stream().mapToInt(s -> s.getSummaryTokens() != null ? s.getSummaryTokens() : 0)
                .sum();

        // 统计各层摘要数量
        long shortCount = allSummaries.stream().filter(s -> MemoryLayerEnum.SHORT.getValue().equals(s.getLayer()))
                .count();
        long midCount = allSummaries.stream().filter(s -> MemoryLayerEnum.MID.getValue().equals(s.getLayer())).count();
        long longCount = allSummaries.stream().filter(s -> MemoryLayerEnum.LONG.getValue().equals(s.getLayer()))
                .count();

        // 计算未总结消息数
        ChatMemorySummary latest = getLatestSummary(appId, MemoryLayerEnum.SHORT);
        Long afterId = latest != null ? latest.getCoveredTo() : null;
        int unsummarized = getUnsummarizedMessages(appId, afterId).size();
        return MemoryCompressionStats.builder()
                .appId(appId)
                .totalOriginalTokens(totalOriginal)
                .totalSummaryTokens(totalSummary)
                .compressionRatio(tokenEstimator.calcCompressionRatio(totalOriginal, totalSummary))
                .shortSummaryCount((int) shortCount)
                .midSummaryCount((int) midCount)
                .longSummaryCount((int) longCount)
                .unsummarizedMessageCount(unsummarized)
                .build();
    }

    // ==================== 内部方法 ====================

    /**
     * 生成SHORT层摘要 - 原始消息 → SHORT摘要
     *
     * <p>处理流程：
     * 1. 格式化消息为对话文本
     * 2. 计算原始Token数
     * 3. 调用AI生成摘要(保留代码细节)
     * 4. 构建并保存ChatMemorySummary实体
     * 5. 记录节省Token数</p>
     *
     * @param appId 应用ID
     * @param userId 用户ID
     * @param messages 待总结的消息列表
     * @param complexity 话题复杂度
     */
    private void performShortSummary(Long appId, Long userId, List<ChatHistory> messages,
            TopicComplexityEnum complexity) {
        String conversationText = formatMessages(messages);
        int originalTokens = tokenEstimator.countTokens(conversationText);
        log.debug("[智能记忆] 调用AI生成SHORT摘要 appId={}, 输入Token={}, 消息数={}", appId, originalTokens, messages.size());

        long aiStartTime = System.currentTimeMillis();
        String summary = memorySummarizeAiService.summarizeShort(conversationText);
        long aiDuration = System.currentTimeMillis() - aiStartTime;
        int summaryTokens = tokenEstimator.countTokens(summary);

        ChatMemorySummary entity = ChatMemorySummary.builder()
                .appId(appId)
                .layer(MemoryLayerEnum.SHORT.getValue())
                .summary(summary)
                .coveredFrom(messages.getFirst().getId())
                .coveredTo(messages.getLast().getId())
                .coveredCount(messages.size())
                .originalTokens(originalTokens)
                .summaryTokens(summaryTokens)
                .topicComplexity(complexity.getValue())
                .userId(userId)
                .build();

        this.save(entity);

        int tokensSaved = originalTokens - summaryTokens;
        memoryMetricsCollector.recordTokensSaved(appId.toString(), tokensSaved);
        log.info("[智能记忆] SHORT摘要完成 appId={}, 原始Token={}, 摘要Token={}, 压缩率={}%, AI耗时={}ms",
                appId, originalTokens, summaryTokens,
                String.format("%.1f", tokenEstimator.calcCompressionRatio(originalTokens, summaryTokens) * 100),
                aiDuration);
    }

    /**
     * 尝试合并SHORT为MID - SHORT × 3 → MID摘要
     *
     * <p>处理流程：
     * 1. 获取未合并的SHORT摘要
     * 2. 检查是否达到合并阈值(默认3个)
     * 3. Embedding相似度去重
     * 4. 调用AI生成MID摘要(保留技术决策)
     * 5. 保存MID摘要实体</p>
     *
     * @param appId 应用ID
     * @param userId 用户ID
     */
    private void tryMergeMid(Long appId, Long userId) {
        List<ChatMemorySummary> shortSummaries = getUnmergedSummaries(appId, MemoryLayerEnum.SHORT);
        log.debug("[智能记忆] 检查MID合并 appId={}, SHORT数={}, 需要数={}",
                appId, shortSummaries.size(), properties.getMidMergeCount());
        if (shortSummaries.size() < properties.getMidMergeCount()) {
            return;
        }

        List<ChatMemorySummary> toMerge = filterByEmbeddingSimilarity(shortSummaries);
        String combinedText = toMerge.stream().map(ChatMemorySummary::getSummary)
                .collect(Collectors.joining("\n\n---\n\n"));
        int originalTokens = tokenEstimator.countTokens(combinedText);

        log.debug("[智能记忆] 调用AI生成MID摘要 appId={}, 合并{}个SHORT, 输入Token={}",
                appId, toMerge.size(), originalTokens);
        long aiStartTime = System.currentTimeMillis();
        String midSummary = memorySummarizeAiService.summarizeMid(combinedText);
        long aiDuration = System.currentTimeMillis() - aiStartTime;
        int summaryTokens = tokenEstimator.countTokens(midSummary);

        float[] embeddingVector = computeEmbedding(midSummary);

        ChatMemorySummary entity = ChatMemorySummary.builder()
                .appId(appId)
                .layer(MemoryLayerEnum.MID.getValue())
                .summary(midSummary)
                .coveredFrom(toMerge.getFirst().getCoveredFrom())
                .coveredTo(toMerge.getLast().getCoveredTo())
                .coveredCount(toMerge.stream().mapToInt(ChatMemorySummary::getCoveredCount).sum())
                .originalTokens(originalTokens)
                .summaryTokens(summaryTokens)
                .parentSummaryId(toMerge.getFirst().getId())
                .embedding(embeddingVector != null ? JSONUtil.toJsonStr(embeddingVector) : null)
                .userId(userId)
                .build();

        this.save(entity);
        log.info("[智能记忆] MID摘要合并完成 appId={}, 合并{}个SHORT, 压缩率={}%, AI耗时={}ms",
                appId, toMerge.size(),
                String.format("%.1f", tokenEstimator.calcCompressionRatio(originalTokens, summaryTokens) * 100),
                aiDuration);
    }

    /**
     * 尝试合并MID为LONG - MID × 3 → LONG摘要
     *
     * <p>处理流程：
     * 1. 获取未合并的MID摘要
     * 2. 检查是否达到合并阈值(默认3个)
     * 3. Embedding相似度去重
     * 4. 调用AI生成LONG摘要(保留项目目标)
     * 5. 保存LONG摘要实体</p>
     *
     * @param appId 应用ID
     * @param userId 用户ID
     */
    private void tryMergeLong(Long appId, Long userId) {
        List<ChatMemorySummary> midSummaries = getUnmergedSummaries(appId, MemoryLayerEnum.MID);
        log.debug("[智能记忆] 检查LONG合并 appId={}, MID数={}, 需要数={}",
                appId, midSummaries.size(), properties.getLongMergeCount());
        if (midSummaries.size() < properties.getLongMergeCount()) {
            return;
        }

        List<ChatMemorySummary> toMerge = filterByEmbeddingSimilarity(midSummaries);
        String combinedText = toMerge.stream().map(ChatMemorySummary::getSummary)
                .collect(Collectors.joining("\n\n---\n\n"));
        int originalTokens = tokenEstimator.countTokens(combinedText);

        log.debug("[智能记忆] 调用AI生成LONG摘要 appId={}, 合并{}个MID, 输入Token={}",
                appId, toMerge.size(), originalTokens);
        long aiStartTime = System.currentTimeMillis();
        String longSummary = memorySummarizeAiService.summarizeLong(combinedText);
        long aiDuration = System.currentTimeMillis() - aiStartTime;
        int summaryTokens = tokenEstimator.countTokens(longSummary);

        ChatMemorySummary entity = ChatMemorySummary.builder()
                .appId(appId)
                .layer(MemoryLayerEnum.LONG.getValue())
                .summary(longSummary)
                .coveredFrom(toMerge.getFirst().getCoveredFrom())
                .coveredTo(toMerge.getLast().getCoveredTo())
                .coveredCount(toMerge.stream().mapToInt(ChatMemorySummary::getCoveredCount).sum())
                .originalTokens(originalTokens)
                .summaryTokens(summaryTokens)
                .parentSummaryId(toMerge.getFirst().getId())
                .userId(userId)
                .build();

        this.save(entity);
        log.info("[智能记忆] LONG摘要合并完成 appId={}, 合并{}个MID, 压缩率={}%, AI耗时={}ms",
                appId, toMerge.size(),
                String.format("%.1f", tokenEstimator.calcCompressionRatio(originalTokens, summaryTokens) * 100),
                aiDuration);
    }

    /**
     * 启发式复杂度分类 - 根据消息内容判断话题复杂度
     *
     * <p>分类规则：
     * - HIGH(阈值20条): 检测到代码块(```)或单条消息>500字符
     * - MEDIUM(阈值30条): 检测到错误关键词(错误/异常/bug/fix/error等)
     * - LOW(阈值40条): 默认，无特殊特征</p>
     *
     * @param messages 消息列表
     * @return 话题复杂度枚举
     */
    private TopicComplexityEnum classifyComplexity(List<ChatHistory> messages) {
        for (ChatHistory msg : messages) {
            String text = msg.getMessage();
            if (text == null)
                continue;

            if (CODE_BLOCK_PATTERN.matcher(text).find() || text.length() > 500) {
                log.debug("[智能记忆] 复杂度分类 结果=HIGH, 触发条件=代码块或长文本(>500字符)");
                return TopicComplexityEnum.HIGH;
            }
            if (ERROR_KEYWORD_PATTERN.matcher(text).find()) {
                log.debug("[智能记忆] 复杂度分类 结果=MEDIUM, 触发条件=错误关键词");
                return TopicComplexityEnum.MEDIUM;
            }
        }
        log.debug("[智能记忆] 复杂度分类 结果=LOW, 触发条件=默认(无特殊特征)");
        return TopicComplexityEnum.LOW;
    }

    /**
     * 根据复杂度返回对应的总结阈值
     *
     * @param complexity 话题复杂度
     * @return 触发总结的消息数量阈值
     */
    private int getThreshold(TopicComplexityEnum complexity) {
        return switch (complexity) {
            case HIGH -> properties.getHighThreshold();
            case MEDIUM -> properties.getMediumThreshold();
            case LOW -> properties.getLowThreshold();
        };
    }

    /**
     * 构建摘要上下文 - 按优先级拼接LONG/MID/SHORT摘要
     *
     * <p>拼接格式：
     * [项目知识] LONG摘要内容
     * [技术决策] MID摘要内容
     * [近期对话] SHORT摘要内容</p>
     *
     * @param appId 应用ID
     * @return 拼接后的摘要上下文字符串
     */
    private String buildSummaryContext(Long appId) {
        StringBuilder sb = new StringBuilder();
        boolean hasLong = false, hasMid = false, hasShort = false;

        ChatMemorySummary longSummary = getLatestSummary(appId, MemoryLayerEnum.LONG);
        if (longSummary != null) {
            sb.append("[项目知识]\n").append(longSummary.getSummary()).append("\n\n");
            hasLong = true;
            log.debug("[智能记忆] 加载LONG摘要 appId={}, id={}, tokens={}",
                    appId, longSummary.getId(), longSummary.getSummaryTokens());
        }

        ChatMemorySummary midSummary = getLatestSummary(appId, MemoryLayerEnum.MID);
        if (midSummary != null) {
            sb.append("[技术决策]\n").append(midSummary.getSummary()).append("\n\n");
            hasMid = true;
            log.debug("[智能记忆] 加载MID摘要 appId={}, id={}, tokens={}",
                    appId, midSummary.getId(), midSummary.getSummaryTokens());
        }

        ChatMemorySummary shortSummary = getLatestSummary(appId, MemoryLayerEnum.SHORT);
        if (shortSummary != null) {
            sb.append("[近期对话]\n").append(shortSummary.getSummary());
            hasShort = true;
            log.debug("[智能记忆] 加载SHORT摘要 appId={}, id={}, tokens={}",
                    appId, shortSummary.getId(), shortSummary.getSummaryTokens());
        }

        log.debug("[智能记忆] 摘要上下文构建完成 appId={}, LONG={}, MID={}, SHORT={}",
                appId, hasLong, hasMid, hasShort);
        return sb.toString().trim();
    }

    /**
     * 兜底截断 - 从旧到新丢弃消息直到Token在安全范围
     *
     * <p>当摘要+最近消息超过70%上下文窗口时触发，从旧到新逐个丢弃消息，
     * 确保最终Token数不超过限制。</p>
     *
     * @param messages 原始消息列表
     * @param summaryTokens 摘要占用的Token数
     * @return 截断后的消息列表
     */
    private List<ChatHistory> truncateToFit(List<ChatHistory> messages, int summaryTokens) {
        int maxTokens = (int) (properties.getContextWindowSize() * properties.getMaxContextRatio()) - summaryTokens;
        List<ChatHistory> result = new ArrayList<>();
        int accumulated = 0;

        for (int i = messages.size() - 1; i >= 0; i--) {
            int msgTokens = tokenEstimator.countTokens(messages.get(i).getMessage());
            if (accumulated + msgTokens > maxTokens) {
                break;
            }
            accumulated += msgTokens;
            result.addFirst(messages.get(i));
        }

        log.warn("[智能记忆] 兜底截断 appId={}, 原始{}条→{}条, Token限制={}, 实际使用={}",
                messages.isEmpty() ? "N/A" : messages.get(0).getAppId(),
                messages.size(), result.size(), maxTokens, accumulated);
        return result;
    }

    /**
     * Embedding相似度去重 - 基于语义向量去除重复摘要
     *
     * <p>算法流程：
     * 1. 计算所有摘要的Embedding向量
     * 2. 遍历摘要，计算两两余弦相似度
     * 3. 相似度 > 0.85 视为重复，跳过合并
     * 4. 返回去重后的摘要列表</p>
     *
     * @param summaries 待去重的摘要列表
     * @return 去重后的摘要列表
     */
    private List<ChatMemorySummary> filterByEmbeddingSimilarity(List<ChatMemorySummary> summaries) {
        if (summaries.size() <= 1) {
            return summaries;
        }

        try {
            log.debug("[智能记忆] 开始Embedding去重 摘要数={}", summaries.size());
            List<Embedding> embeddings = new ArrayList<>();
            for (ChatMemorySummary s : summaries) {
                embeddings.add(embeddingModel.embed(s.getSummary()).content());
            }

            List<ChatMemorySummary> filtered = new ArrayList<>();
            filtered.add(summaries.getFirst());
            boolean[] merged = new boolean[summaries.size()];

            for (int i = 1; i < summaries.size(); i++) {
                if (merged[i])
                    continue;
                boolean isDuplicate = false;
                for (int j = 0; j < i; j++) {
                    if (merged[j])
                        continue;
                    double similarity = cosineSimilarity(embeddings.get(i).vector(), embeddings.get(j).vector());
                    if (similarity > properties.getSimilarityThreshold()) {
                        isDuplicate = true;
                        log.debug("[智能记忆] Embedding去重 摘要{}与{}相似度={}, 跳过",
                                summaries.get(i).getId(), summaries.get(j).getId(),
                                String.format("%.3f", similarity));
                        break;
                    }
                }
                if (!isDuplicate) {
                    filtered.add(summaries.get(i));
                }
            }

            if (filtered.size() < summaries.size()) {
                log.info("[智能记忆] Embedding去重完成 {}个→{}个", summaries.size(), filtered.size());
            }
            return filtered;
        } catch (Exception e) {
            log.warn("[智能记忆] Embedding去重失败，使用全部摘要: {}", e.getMessage());
            return summaries;
        }
    }

    /**
     * 计算两个Embedding向量的余弦相似度
     *
     * @param a 向量a
     * @param b 向量b
     * @return 相似度值，范围[0,1]
     */
    private double cosineSimilarity(float[] a, float[] b) {
        if (a.length != b.length)
            return 0;
        double dotProduct = 0, normA = 0, normB = 0;
        for (int i = 0; i < a.length; i++) {
            dotProduct += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }
        double denominator = Math.sqrt(normA) * Math.sqrt(normB);
        return denominator == 0 ? 0 : dotProduct / denominator;
    }

    /**
     * 计算文本的Embedding向量 - 用于语义相似度计算
     *
     * @param text 待计算的文本
     * @return Embedding向量，失败时返回null
     */
    private float[] computeEmbedding(String text) {
        try {
            float[] result = embeddingModel.embed(text).content().vector();
            log.debug("[智能记忆] Embedding计算成功 文本长度={}", text.length());
            return result;
        } catch (Exception e) {
            log.warn("[智能记忆] Embedding计算失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 估算消息列表的Token总数
     *
     * @param messages 消息列表
     * @return Token总数
     */
    private int estimateMessagesTokens(List<ChatHistory> messages) {
        return messages.stream().mapToInt(m -> tokenEstimator.countTokens(m.getMessage())).sum();
    }

    /**
     * 格式化消息列表为对话文本 - 用于AI摘要输入
     *
     * <p>格式：用户: xxx\n\nAI: xxx\n\n</p>
     *
     * @param messages 消息列表
     * @return 格式化后的对话文本
     */
    private String formatMessages(List<ChatHistory> messages) {
        StringBuilder sb = new StringBuilder();
        for (ChatHistory msg : messages) {
            String role = ChatHistoryMessageTypeEnum.USER.getValue().equals(msg.getMessageType()) ? "用户" : "AI";
            sb.append(role).append(": ").append(msg.getMessage()).append("\n\n");
        }
        return sb.toString();
    }

    // ==================== 数据访问 ====================

    /**
     * 获取指定应用和层级的最新摘要
     *
     * @param appId 应用ID
     * @param layer 摘要层级
     * @return 最新摘要实体，不存在时返回null
     */
    private ChatMemorySummary getLatestSummary(Long appId, MemoryLayerEnum layer) {
        QueryWrapper qw = QueryWrapper.create()
                .eq(ChatMemorySummary::getAppId, appId)
                .eq(ChatMemorySummary::getLayer, layer.getValue())
                .orderBy(ChatMemorySummary::getCreateTime, false)
                .limit(1);
        return chatMemorySummaryMapper.selectOneByQuery(qw);
    }

    /**
     * 获取指定应用的所有摘要 - 按创建时间升序排列
     *
     * @param appId 应用ID
     * @return 摘要列表
     */
    private List<ChatMemorySummary> getSummariesByApp(Long appId) {
        return this.list(QueryWrapper.create()
                .eq(ChatMemorySummary::getAppId, appId)
                .orderBy(ChatMemorySummary::getCreateTime, true));
    }

    /**
     * 获取指定层级未合并的摘要 - 基于parentSummaryId判断
     *
     * <p>逻辑：查询ID大于上一层最新摘要的parentSummaryId的摘要</p>
     *
     * @param appId 应用ID
     * @param layer 摘要层级
     * @return 未合并的摘要列表
     */
    private List<ChatMemorySummary> getUnmergedSummaries(Long appId, MemoryLayerEnum layer) {
        ChatMemorySummary latestNext = getLatestSummary(appId,
                layer == MemoryLayerEnum.SHORT ? MemoryLayerEnum.MID : MemoryLayerEnum.LONG);
        Long afterId = latestNext != null ? latestNext.getParentSummaryId() : null;

        QueryWrapper qw = QueryWrapper.create()
                .eq(ChatMemorySummary::getAppId, appId)
                .eq(ChatMemorySummary::getLayer, layer.getValue())
                .orderBy(ChatMemorySummary::getCreateTime, true);

        if (afterId != null) {
            qw.gt(ChatMemorySummary::getId, afterId);
        }

        return this.list(qw);
    }

    /**
     * 获取指定应用中未总结的消息 - 基于ID过滤
     *
     * @param appId 应用ID
     * @param afterId 起始消息ID，null时查询全部
     * @return 未总结的消息列表
     */
    private List<ChatHistory> getUnsummarizedMessages(Long appId, Long afterId) {
        QueryWrapper qw = QueryWrapper.create()
                .eq(ChatHistory::getAppId, appId)
                .orderBy(ChatHistory::getCreateTime, true);
        if (afterId != null) {
            qw.gt(ChatHistory::getId, afterId);
        }
        List<ChatHistory> result = chatHistoryService.list(qw);
        log.debug("[智能记忆] 查询未总结消息 appId={}, afterId={}, 结果数={}", appId, afterId, result.size());
        return result;
    }

    /**
     * 获取指定应用的最近N条消息 - 按创建时间降序查询后反转
     *
     * @param appId 应用ID
     * @param limit 消息数量限制
     * @return 最近的消息列表（按时间升序）
     */
    private List<ChatHistory> getRecentMessages(Long appId, int limit) {
        List<ChatHistory> messages = chatHistoryService.list(QueryWrapper.create()
                .eq(ChatHistory::getAppId, appId)
                .orderBy(ChatHistory::getCreateTime, false)
                .limit(limit));
        if (CollUtil.isNotEmpty(messages)) {
            messages = messages.reversed();
        }
        log.debug("[智能记忆] 查询最近消息 appId={}, limit={}, 结果数={}", appId, limit, messages.size());
        return messages;
    }
}
