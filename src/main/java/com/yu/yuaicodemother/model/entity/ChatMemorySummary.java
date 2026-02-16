package com.yu.yuaicodemother.model.entity;

import com.mybatisflex.annotation.*;
import com.mybatisflex.core.keygen.KeyGenerators;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 对话记忆摘要实体类 - 智能记忆管理系统的核心数据表
 *
 * <p>功能说明：
 * 本实体对应 chat_memory_summary 表，用于存储 AI 对话的历史摘要信息。
 * 通过分层摘要机制（SHORT/MID/LONG）实现 Token 压缩，解决长对话上下文超限问题。</p>
 *
 * <p>核心设计：
 * 1. 三层摘要结构：SHORT(短期事实记录) → MID(中期技术决策) → LONG(长期项目知识)
 * 2. 乐观锁机制：version 字段配合 @Column(version = true) 防止并发冲突
 * 3. Embedding 向量：存储摘要的向量表示，用于相似度去重
 * 4. 溯源能力：parentSummaryId 记录合并来源，便于调试</p>
 *
 * <p>使用场景：
 * - 对话完成后异步触发总结，将原始消息压缩为摘要
 * - 加载对话历史时，优先加载高级别摘要 + 最近原始消息
 * - 达到压缩率 80%-90%，显著降低 Token 消耗</p>
 *
 * @author 智能记忆管理系统
 * @see com.yu.yuaicodemother.model.enums.MemoryLayerEnum
 * @see com.yu.yuaicodemother.service.SmartMemoryService
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("chat_memory_summary")
public class ChatMemorySummary implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID - 雪花算法生成
     */
    @Id(keyType = KeyType.Generator, value = KeyGenerators.snowFlakeId)
    private Long id;

    /**
     * 应用ID - 关联app表，标识该摘要属于哪个应用
     */
    @Column("appId")
    private Long appId;

    /**
     * 摘要层级 - 可选值：SHORT/MID/LONG
     * SHORT: 短期摘要，保留代码、变量名等事实
     * MID: 中期摘要，保留技术决策和选型理由
     * LONG: 长期摘要，保留项目目标和架构方向
     */
    private String layer;

    /**
     * 摘要内容 - AI生成的对话压缩文本
     */
    private String summary;

    /**
     * 覆盖起始消息ID - 该摘要包含的最早消息
     */
    @Column("coveredFrom")
    private Long coveredFrom;

    /**
     * 覆盖结束消息ID - 该摘要包含的最晚消息
     */
    @Column("coveredTo")
    private Long coveredTo;

    /**
     * 覆盖消息数量 - 统计被压缩的原始消息数
     */
    @Column("coveredCount")
    private Integer coveredCount;

    /**
     * 原始Token数 - 被压缩消息的JTokkit精确计数，用于计算压缩率
     */
    @Column("originalTokens")
    private Integer originalTokens;

    /**
     * 摘要Token数 - 生成摘要的Token数量
     */
    @Column("summaryTokens")
    private Integer summaryTokens;

    /**
     * 主题复杂度 - 可选值：LOW/MEDIUM/HIGH
     * 决定触发总结的阈值：LOW(40条)/MEDIUM(30条)/HIGH(20条)
     */
    @Column("topicComplexity")
    private String topicComplexity;

    /**
     * 父摘要ID - 记录MID/LONG摘要由哪些下级摘要合并而来，用于溯源
     */
    @Column("parentSummaryId")
    private Long parentSummaryId;

    /**
     * Embedding向量(JSON格式) - 摘要的语义向量表示
     * 用于合并时计算相似度，去除冗余内容
     */
    private String embedding;

    /**
     * 乐观锁版本号 - 防止异步总结时的并发冲突
     */
    @Column(version = true)
    private Integer version;

    /**
     * 用户ID - 摘要创建者
     */
    @Column("userId")
    private Long userId;

    /**
     * 创建时间
     */
    @Column("createTime")
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @Column("updateTime")
    private LocalDateTime updateTime;

    /**
     * 是否删除 - 逻辑删除标记
     */
    @Column(value = "isDelete", isLogicDelete = true)
    private Integer isDelete;
}
