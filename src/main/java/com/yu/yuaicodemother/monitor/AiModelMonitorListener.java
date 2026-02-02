package com.yu.yuaicodemother.monitor;

import dev.langchain4j.model.chat.listener.ChatModelErrorContext;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import dev.langchain4j.model.chat.listener.ChatModelRequestContext;
import dev.langchain4j.model.chat.listener.ChatModelResponseContext;
import dev.langchain4j.model.output.TokenUsage;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

/**
 * AI 模型交互监听器
 * <p>
 * 该类实现了 LangChain4j 的 {@link ChatModelListener} 接口，作为全局拦截器。
 * 它的主要职责是：
 * 1. 监听 AI 模型的 Request、Response 和 Error 事件。
 * 2. 维护请求上下文（解决跨线程参数传递问题）。
 * 3. 调用 {@link AiModelMetricsCollector} 进行各项指标的埋点上报。
 * </p>
 *
 * @author 鱼🐟
 * @version 1.0
 */
@Component
@Slf4j
public class AiModelMonitorListener implements ChatModelListener {


    @Resource
    private AiModelMetricsCollector aiModelMetricsCollector;
    /**
     * 属性键：请求开始时间
     * 用于在 attributes Map 中存储 Instant 对象，以便在响应/错误时计算耗时。
     */
    private static final String REQUEST_START_TIME_KEY = "request_start_time";

    /**
     * 属性键：监控上下文
     * <p>
     * 关键设计：LangChain4j 的 onRequest 和 onResponse 可能不在同一个线程执行（例如流式响应）。
     * 因此，不能依赖 ThreadLocal。我们需要将 {@link MonitorContext} 放入 request attributes 中，
     * 随着请求链路传递到 response 回调中。
     * </p>
     */
    private static final String MONITOR_CONTEXT_KEY = "monitor_context";



    /**
     * 请求发起前的回调
     * <p>
     * 触发时机：在向 LLM 发送 HTTP 请求之前。
     * 核心动作：
     * 1. 记录开始时间。
     * 2. 将 ThreadLocal 中的用户信息“快照”保存到 Request Attributes 中。
     * 3. 记录 "started" 状态的指标。
     * </p>
     */
    @Override
    public void onRequest(ChatModelRequestContext requestContext) {

        // 1. 记录请求开始时间（用于后续计算 Latency）
        requestContext.attributes().put(REQUEST_START_TIME_KEY, Instant.now());

        // 2. 获取当前线程的监控上下文（包含 userId, appId 等）
        // 注意：MonitorContextHolder 通常基于 ThreadLocal
        MonitorContext context = MonitorContextHolder.getContext();

        // 3. 【关键】将上下文快照存入 attributes，防止跨线程丢失
        if (context != null) {
            requestContext.attributes().put(MONITOR_CONTEXT_KEY, context);
        }

        String userId = (context != null) ? context.getUserId() : "unknown";
        String appId = (context != null) ? context.getAppId() : "unknown";

        // 4. 获取模型名称 (e.g., "gpt-4")
        String modelName = requestContext.chatRequest().modelName();

        // 5. 埋点：记录请求已开始
        aiModelMetricsCollector.recordRequest(userId, appId, modelName, "started");
    }

    /**
     * 请求成功响应后的回调
     * <p>
     * 触发时机：收到 LLM 的完整响应后。
     * 核心动作：
     * 1. 恢复上下文（从 attributes 中取出）。
     * 2. 记录 "success" 状态。
     * 3. 结算耗时和 Token 消耗。
     * </p>
     */
    @Override
    public void onResponse(ChatModelResponseContext responseContext) {
        // 1. 获取请求生命周期内的属性容器
        Map<Object, Object> attributes = responseContext.attributes();

        // 2. 【关键】从属性中恢复监控上下文（而不是从 MonitorContextHolder 获取，因为线程可能变了）
        MonitorContext context = (MonitorContext) attributes.get(MONITOR_CONTEXT_KEY);

        // 防御性编程：防止 context 为空（虽然理论上 onRequest 必先执行）
        String userId = (context != null) ? context.getUserId() : "unknown";
        String appId = (context != null) ? context.getAppId() : "unknown";

        // 3. 获取实际响应的模型名称
        String modelName = responseContext.chatResponse().modelName();

        // 4. 埋点：记录请求成功
        aiModelMetricsCollector.recordRequest(userId, appId, modelName, "success");

        // 5. 埋点：记录响应时间
        recordResponseTime(attributes, userId, appId, modelName);

        // 6. 埋点：记录 Token 消耗
        recordTokenUsage(responseContext, userId, appId, modelName);
    }

    /**
     * 请求发生异常时的回调
     * <p>
     * 触发时机：网络超时、API 密钥错误或模型拒绝服务时。
     * </p>
     */
    @Override
    public void onError(ChatModelErrorContext errorContext) {

        // 1. 获取请求生命周期内的属性容器
        Map<Object, Object> attributes = errorContext.attributes();

        // 2. 【关键】从属性中恢复监控上下文
        MonitorContext context = (MonitorContext) attributes.get(MONITOR_CONTEXT_KEY);

        String userId = (context != null) ? context.getUserId() : "unknown";
        String appId = (context != null) ? context.getAppId() : "unknown";

        // 获取模型名称
        String modelName = errorContext.chatRequest().modelName();

        // 获取错误详情
        // ⚠️ 注意：ErrorMessage 可能包含动态内容，建议在 Collector 中进行归一化处理，防止 Tag 基数爆炸
        String errorMessage = errorContext.error().getMessage();

        // 3. 埋点：记录请求失败
        aiModelMetricsCollector.recordRequest(userId, appId, modelName, "error");

        // 4. 埋点：记录具体的错误原因
        aiModelMetricsCollector.recordError(userId, appId, modelName, errorMessage);

        // 5. 埋点：即使失败，也记录耗时（用于分析超时等问题）
        recordResponseTime(attributes, userId, appId, modelName);
    }


    /**
     * 辅助方法：计算并记录响应耗时
     *
     * @param attributes 请求属性上下文
     * @param userId     用户ID
     * @param appId      应用ID
     * @param modelName  模型名称
     */
    private void recordResponseTime(Map<Object, Object> attributes, String userId, String appId, String modelName) {
        Object startTimeObj = attributes.get(REQUEST_START_TIME_KEY);
        if (startTimeObj instanceof Instant) {
            Instant startTime = (Instant) startTimeObj;
            // 计算时间差：Now - Start
            Duration responseTime = Duration.between(startTime, Instant.now());
            aiModelMetricsCollector.recordResponseTime(userId, appId, modelName, responseTime);
        }
    }

    /**
     * 辅助方法：提取并记录 Token 使用情况
     *
     * @param responseContext 响应上下文
     * @param userId          用户ID
     * @param appId           应用ID
     * @param modelName       模型名称
     */
    private void recordTokenUsage(ChatModelResponseContext responseContext, String userId, String appId, String modelName) {
        // LangChain4j 标准化了 TokenUsage 对象
        TokenUsage tokenUsage = responseContext.chatResponse().metadata().tokenUsage();

        if (tokenUsage != null) {
            // 记录 Prompt (输入) Token
            aiModelMetricsCollector.recordTokenUsage(userId, appId, modelName, "input", tokenUsage.inputTokenCount());


            // 记录 Completion (输出) Token
            aiModelMetricsCollector.recordTokenUsage(userId, appId, modelName, "output", tokenUsage.outputTokenCount());

            // 记录 Total Token
            aiModelMetricsCollector.recordTokenUsage(userId, appId, modelName, "total", tokenUsage.totalTokenCount());
        }
    }
}