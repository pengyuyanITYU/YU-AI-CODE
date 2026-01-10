package com.yu.yuaicodemother.monitor;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * AI 模型核心指标收集器
 * <p>
 * 该组件负责对接 Micrometer (Prometheus)，将 AI 服务的业务指标进行埋点上报。
 * 涵盖四个核心维度：
 * 1. {@link #recordRequest} - 流量监控 (QPS/调用量)
 * 2. {@link #recordError} - 稳定性监控 (错误率/异常分布)
 * 3. {@link #recordTokenUsage} - 成本监控 (Token 消耗统计)
 * 4. {@link #recordResponseTime} - 性能监控 (响应耗时/延迟)
 * </p>
 *
 * @author 鱼🐟
 * @version 1.0
 */
@Component
@Slf4j
public class AiModelMetricsCollector {

    /**
     * Micrometer 核心注册表，用于注册和管理所有的指标仪表盘
     */
    @Resource
    private MeterRegistry meterRegistry;

    // =================================================================================
    // 本地缓存层
    // 设计目的：虽然 MeterRegistry 内部有缓存，但在高并发场景下，
    // 在应用层维护一个 Map 可以减少 Builder 对象的重复创建和查找开销。
    // =================================================================================

    /** 请求计数器缓存：Key = userId_appId_modelName_status */
    private final ConcurrentMap<String, Counter> requestCountersCache = new ConcurrentHashMap<>();

    /** 错误计数器缓存：Key = userId_appId_modelName_errorMessage */
    private final ConcurrentMap<String, Counter> errorCountersCache = new ConcurrentHashMap<>();

    /** Token计数器缓存：Key = userId_appId_modelName_tokenType */
    private final ConcurrentMap<String, Counter> tokenCountersCache = new ConcurrentHashMap<>();

    /** 响应时间记录器缓存：Key = userId_appId_modelName */
    private final ConcurrentMap<String, Timer> responseTimersCache = new ConcurrentHashMap<>();

    /**
     * 记录 AI 模型的请求次数 (Counter)
     * <p>
     * 用于统计总调用量、计算 QPS 以及分析不同模型的调用热度。
     * </p>
     *
     * @param userId    调用用户的ID (建议：如果用户量过百万，建议仅记录租户ID或不做Tag，防止基数爆炸)
     * @param appId     接入应用的ID
     * @param modelName 模型名称 (如: gpt-4, claude-3-opus)
     * @param status    请求状态 (如: "SUCCESS", "FAILED", "TIMEOUT")，用于计算成功率
     */
    public void recordRequest(String userId, String appId, String modelName, String status) {
        // 拼接唯一Key，用于在本地 Map 中查找是否已存在该 Counter
        String key = String.format("%s_%s_%s_%s", userId, appId, modelName, status);

        // computeIfAbsent 保证线程安全：如果 Key 不存在则创建并注册，存在则直接返回
        Counter counter = requestCountersCache.computeIfAbsent(key, k ->
                Counter.builder("ai_model_requests_total") // 指标名称 (Prometheus 中显示的 metric name)
                        .description("AI模型总请求次数")       // 指标描述
                        .tag("user_id", userId)            // 维度标签：用户
                        .tag("app_id", appId)              // 维度标签：应用
                        .tag("model_name", modelName)      // 维度标签：模型
                        .tag("status", status)             // 维度标签：状态
                        .register(meterRegistry)           // 注册到 Micrometer
        );

        // 计数器 +1
        counter.increment();
    }

    /**
     * 记录 AI 模型的异常次数 (Counter)
     * <p>
     * 用于监控服务稳定性，配合 AlertManager 配置告警规则。
     * </p>
     *
     * <h3>⚠️ 警告 (High Cardinality Warning)</h3>
     * 请勿将包含动态内容（如时间戳、请求ID、堆栈详情）的原始错误信息直接传入 {@code errorMessage}。
     * 必须先进行归一化处理（例如将 "Timeout at 12:00" 转换为 "TIMEOUT_ERROR"），
     * 否则会导致监控系统内存溢出。
     *
     * @param userId       调用用户的ID
     * @param appId        接入应用的ID
     * @param modelName    模型名称
     * @param errorMessage 错误类型摘要 (如: "API_KEY_INVALID", "CONTEXT_LENGTH_EXCEEDED")
     */
    public void recordError(String userId, String appId, String modelName, String errorMessage) {
        String key = String.format("%s_%s_%s_%s", userId, appId, modelName, errorMessage);
        Counter counter = errorCountersCache.computeIfAbsent(key, k ->
                Counter.builder("ai_model_errors_total")
                        .description("AI模型错误次数")
                        .tag("user_id", userId)
                        .tag("app_id", appId)
                        .tag("model_name", modelName)
                        .tag("error_message", errorMessage) // 关键标签：错误原因
                        .register(meterRegistry)
        );

        counter.increment();
    }

    /**
     * 记录 Token 消耗量 (Counter)
     * <p>
     * 用于成本核算和模型产出分析。与请求次数不同，这里是累加具体的数值。
     * </p>
     *
     * @param userId     调用用户的ID
     * @param appId      接入应用的ID
     * @param modelName  模型名称
     * @param tokenType  Token类型 (通常为 "Input" 或 "Prompt" 表示输入，"Output" 或 "Completion" 表示输出)
     * @param tokenCount 本次请求消耗的 Token 数量
     */
    public void recordTokenUsage(String userId, String appId, String modelName,
                                 String tokenType, long tokenCount) {
        // 过滤掉无效数据，避免污染指标
        if (tokenCount <= 0) {
            return;
        }

        String key = String.format("%s_%s_%s_%s", userId, appId, modelName, tokenType);

        Counter counter = tokenCountersCache.computeIfAbsent(key, k ->
                Counter.builder("ai_model_tokens_total")
                        .description("AI模型Token消耗总数")
                        .tag("user_id", userId)
                        .tag("app_id", appId)
                        .tag("model_name", modelName)
                        .tag("token_type", tokenType) // 区分是提问消耗还是回答消耗
                        .register(meterRegistry)
        );

        // 累加指定的 Token 数量
        counter.increment(tokenCount);
    }

    /**
     * 记录请求响应时间 (Timer)
     * <p>
     * Timer 会自动记录：
     * 1. count: 调用总次数
     * 2. sum: 总耗时
     * 3. max: 最大耗时
     * (若配置了直方图，还能计算 P99, P95 等分位数)
     * </p>
     *
     * @param userId    调用用户的ID
     * @param appId     接入应用的ID
     * @param modelName 模型名称
     * @param duration  本次请求的耗时对象 (Duration.ofMillis(xxx))
     */
    public void recordResponseTime(String userId, String appId, String modelName, Duration duration) {
        String key = String.format("%s_%s_%s", userId, appId, modelName);

        Timer timer = responseTimersCache.computeIfAbsent(key, k ->
                Timer.builder("ai_model_response_duration_seconds")
                        .description("AI模型响应时间")
                        .tag("user_id", userId)
                        .tag("app_id", appId)
                        .tag("model_name", modelName)
                        // 注意：Timer 默认单位通常是秒，Micrometer 会自动处理单位转换
                        .register(meterRegistry)
        );

        // 记录本次耗时
        timer.record(duration);
    }
}