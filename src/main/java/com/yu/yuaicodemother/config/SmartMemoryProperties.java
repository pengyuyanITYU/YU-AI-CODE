package com.yu.yuaicodemother.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 智能记忆配置属性 - YAML配置映射
 *
 * <p>配置项说明：
 * - enabled: 是否启用智能记忆(默认true)
 * - low/medium/highThreshold: 各复杂度触发阈值(默认40/30/20条)
 * - recentMessages: 加载时保留的最近原始消息数(默认10条)
 * - mid/longMergeCount: 合并为MID/LONG所需的摘要数(默认3个)
 * - similarityThreshold: Embedding相似度去重阈值(默认0.85)
 * - maxContextRatio: 上下文窗口最大使用比例(默认0.7即70%)
 * - contextWindowSize: 模型上下文窗口大小(默认128000)
 * - useAiClassification: 是否使用AI精细分类(默认false)</p>
 *
 * <p>YAML示例：
 * <pre>
 * smart-memory:
 *   enabled: true
 *   low-threshold: 40
 *   high-threshold: 20
 *   recent-messages: 10
 * </pre></p>
 *
 * @see org.springframework.boot.context.properties.ConfigurationProperties
 */
@Data
@Component
@ConfigurationProperties(prefix = "smart-memory")
public class SmartMemoryProperties {

    private boolean enabled = true;
//    enabled: 是否启用智能记忆(默认true)

    private int lowThreshold = 1;
    private int mediumThreshold = 1;
    private int highThreshold = 1;
//    low/medium/highThreshold: 各复杂度触发阈值(默认40/30/20条)

    private int recentMessages = 10;
//    recentMessages: 加载时保留的最近原始消息数(默认10条)

    private int midMergeCount = 2;
    private int longMergeCount = 2;
//    mid/longMergeCount: 合并为MID/LONG所需的摘要数(默认3个)

    private double similarityThreshold = 0.85;
//    similarityThreshold: Embedding相似度去重阈值(默认0.85)

    private double maxContextRatio = 0.7;
//    maxContextRatio: 上下文窗口最大使用比例(默认0.7即70%)

    private int contextWindowSize = 128000;
//    contextWindowSize: 模型上下文窗口大小(默认128000)

    private boolean useAiClassification = false;
//    useAiClassification: 是否使用AI精细分类(默认false)
}
