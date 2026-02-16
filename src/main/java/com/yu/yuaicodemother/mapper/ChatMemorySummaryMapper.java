package com.yu.yuaicodemother.mapper;

import com.mybatisflex.core.BaseMapper;
import com.yu.yuaicodemother.model.entity.ChatMemorySummary;
import org.apache.ibatis.annotations.Mapper;

/**
 * 对话记忆摘要 Mapper 接口
 *
 * <p>功能说明：
 * 基于 MyBatis-Flex 的数据访问层，提供 ChatMemorySummary 实体的 CRUD 操作。
 * 继承 BaseMapper 自动获得基础增删改查能力。</p>
 *
 * <p>数据表：chat_memory_summary
 * - 主键：id (BIGINT, 雪花算法)
 * - 核心字段：appId, layer, summary, coveredFrom/To
 * - 统计字段：originalTokens, summaryTokens, coveredCount
 * - 扩展字段：embedding(JSON), topicComplexity, parentSummaryId
 * - 并发控制：version (乐观锁)</p>
 *
 * <p>使用场景：
 * - SmartMemoryServiceImpl 通过此 Mapper 查询/保存摘要数据
 * - 索引优化：idx_appId_layer, idx_appId_createTime</p>
 *
 * @see com.yu.yuaicodemother.model.entity.ChatMemorySummary
 * @see com.yu.yuaicodemother.service.impl.SmartMemoryServiceImpl
 */
@Mapper
public interface ChatMemorySummaryMapper extends BaseMapper<ChatMemorySummary> {
}
