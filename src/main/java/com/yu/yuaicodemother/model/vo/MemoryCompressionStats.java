package com.yu.yuaicodemother.model.vo;

import lombok.Builder;
import lombok.Data;

/**
 * 记忆压缩统计VO - 用于展示和监控
 *
 * <p>统计字段：
 * - appId: 应用ID
 * - totalOriginalTokens: 原始消息Token总数
 * - totalSummaryTokens: 摘要Token总数
 * - compressionRatio: 压缩率(1 - 摘要/原始)
 * - short/mid/longSummaryCount: 各层摘要数量
 * - unsummarizedMessageCount: 未总结消息数</p>
 *
 * <p>使用场景：
 * - 管理后台展示应用的记忆统计
 * - 效果评估和成本分析</p>
 *
 * @see com.yu.yuaicodemother.service.SmartMemoryService#getCompressionStats
 */
@Data
@Builder
public class MemoryCompressionStats {

    private Long appId;
    private int totalOriginalTokens; // 原始消息Token总数
    private int totalSummaryTokens; // 摘要Token总数
    private double compressionRatio; // 压缩率(1 - 摘要/原始)
    private int shortSummaryCount;
    private int midSummaryCount;
    private int longSummaryCount; // 各层摘要数量
    private int unsummarizedMessageCount; //未总结消息数
}
