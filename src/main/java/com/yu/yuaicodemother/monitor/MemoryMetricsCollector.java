package com.yu.yuaicodemother.monitor;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 智能记忆监控指标收集器 - 基于Micrometer
 *
 * <p>监控指标：
 * 1. smart_memory_summary_total (Counter): 总结执行次数，按appId标签
 * 2. smart_memory_summary_duration (Timer): 总结耗时，用于性能监控
 * 3. smart_memory_tokens_saved (Counter): 单次节省Token数
 * 4. smart_memory_tokens_saved_total (Gauge): 累计节省Token总数
 * 5. smart_memory_load_hit_rate (Gauge): 摘要命中缓存率
 * 6. smart_memory_compression_ratio (Gauge): 平均压缩率</p>
 *
 * <p>查看方式：
 * - Prometheus: http://localhost:8080/actuator/prometheus
 * - Grafana: 导入JVM仪表盘查看</p>
 *
 * @see io.micrometer.core.instrument.MeterRegistry
 * @see com.yu.yuaicodemother.service.impl.SmartMemoryServiceImpl
 */
@Slf4j
@Component
public class MemoryMetricsCollector {

    @Resource
    private MeterRegistry meterRegistry;

    private final AtomicLong totalTokensSaved = new AtomicLong(0);
    private final AtomicInteger loadHitCount = new AtomicInteger(0);
    private final AtomicInteger loadTotalCount = new AtomicInteger(0);
    private final ConcurrentHashMap<String, Double> compressionRatios = new ConcurrentHashMap<>();

    /**
     * 记录总结执行次数和耗时
     *
     * <p>指标：
     * - smart_memory_summary_total: 总结执行次数
     * - smart_memory_summary_duration: 总结耗时</p>
     *
     * @param appId 应用ID
     * @param durationMs 执行耗时(毫秒)
     */
    public void recordSummaryExecution(String appId, long durationMs) {
        Counter.builder("smart_memory_summary_total")
                .tag("appId", appId)
                .register(meterRegistry)
                .increment();

        Timer.builder("smart_memory_summary_duration")
                .tag("appId", appId)
                .register(meterRegistry)
                .record(Duration.ofMillis(durationMs));

        log.debug("[智能记忆指标] 记录执行 appId={}, 耗时={}ms", appId, durationMs);
    }

    /**
     * 记录节省Token数和累计值
     *
     * <p>指标：
     * - smart_memory_tokens_saved: 单次节省Token数
     * - smart_memory_tokens_saved_total: 累计节省Token总数</p>
     *
     * @param appId 应用ID
     * @param tokensSaved 本次节省的Token数
     */
    public void recordTokensSaved(String appId, int tokensSaved) {
        totalTokensSaved.addAndGet(tokensSaved);

        Counter.builder("smart_memory_tokens_saved")
                .tag("appId", appId)
                .register(meterRegistry)
                .increment(tokensSaved);

        Gauge.builder("smart_memory_tokens_saved_total", totalTokensSaved, AtomicLong::doubleValue)
                .register(meterRegistry);

        log.debug("[智能记忆指标] 记录节省Token appId={}, 节省={}, 累计={}", appId, tokensSaved, totalTokensSaved.get());
    }

    /**
     * 记录摘要命中率和命中次数
     *
     * <p>指标：
     * - smart_memory_load_hit_rate: 摘要命中率</p>
     *
     * @param appId 应用ID
     * @param hitSummary 是否命中摘要
     */
    public void recordLoadHit(String appId, boolean hitSummary) {
        loadTotalCount.incrementAndGet();
        if (hitSummary) {
            loadHitCount.incrementAndGet();
        }

        Gauge.builder("smart_memory_load_hit_rate", this,
                c -> loadTotalCount.get() == 0 ? 0 : (double) loadHitCount.get() / loadTotalCount.get())
                .register(meterRegistry);

        log.debug("[智能记忆指标] 记录加载命中 appId={}, 命中={}, 累计命中={}/{}",
                appId, hitSummary, loadHitCount.get(), loadTotalCount.get());
    }

    /**
     * 记录压缩率 - 用于监控平均压缩效果
     *
     * <p>指标：
     * - smart_memory_compression_ratio: 平均压缩率</p>
     *
     * @param appId 应用ID
     * @param ratio 压缩率(0-1)
     */
    public void recordCompressionRatio(String appId, double ratio) {
        compressionRatios.put(appId, ratio);

        Gauge.builder("smart_memory_compression_ratio", compressionRatios,
                m -> m.values().stream().mapToDouble(Double::doubleValue).average().orElse(0))
                .register(meterRegistry);

        log.debug("[智能记忆指标] 记录压缩率 appId={}, 压缩率={}%", appId, String.format("%.1f", ratio * 100));
    }
}
