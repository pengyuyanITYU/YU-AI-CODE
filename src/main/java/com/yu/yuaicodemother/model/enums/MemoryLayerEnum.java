package com.yu.yuaicodemother.model.enums;

import cn.hutool.core.util.ObjUtil;
import lombok.Getter;

/**
 * 记忆层级枚举 - 定义智能记忆管理系统的三层摘要结构
 *
 * <p>层级说明：
 * 1. SHORT(短期摘要): 最近对话的事实记录，保留代码、变量名、配置参数
 *    - 触发阈值：根据复杂度20-40条消息
 *    - 保留内容：代码片段、SQL语句、错误堆栈、具体数值
 *
 * 2. MID(中期摘要): 技术决策记录，保留选型理由和架构决策
 *    - 生成方式：SHORT × 3 合并(去重后)
 *    - 保留内容：技术选型、决策原因、被否决方案
 *
 * 3. LONG(长期摘要): 项目级知识，保留最终目标和核心需求
 *    - 生成方式：MID × 3 合并(去重后)
 *    - 保留内容：项目目标、技术架构、关键约束</p>
 *
 * <p>晋升机制：
 * SHORT(3个) → 合并为MID(去重) → LONG(去重)</p>
 *
 * <p>加载优先级：
 * 构建AI上下文时按 LONG → MID → SHORT 顺序加载，再补充最近原始消息</p>
 *
 * @author 智能记忆管理系统
 * @see com.yu.yuaicodemother.model.entity.ChatMemorySummary
 * @see com.yu.yuaicodemother.service.SmartMemoryService
 */
@Getter
public enum MemoryLayerEnum {

    SHORT("短期摘要", "SHORT"),
    MID("中期摘要", "MID"),
    LONG("长期摘要", "LONG");

    private final String text;
    private final String value;

    MemoryLayerEnum(String text, String value) {
        this.text = text;
        this.value = value;
    }

    public static MemoryLayerEnum getEnumByValue(String value) {
        if (ObjUtil.isEmpty(value)) {
            return null;
        }
        for (MemoryLayerEnum anEnum : MemoryLayerEnum.values()) {
            if (anEnum.value.equals(value)) {
                return anEnum;
            }
        }
        return null;
    }
}
