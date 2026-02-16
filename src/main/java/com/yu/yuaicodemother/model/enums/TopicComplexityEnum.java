package com.yu.yuaicodemother.model.enums;

import cn.hutool.core.util.ObjUtil;
import lombok.Getter;

/**
 * 主题复杂度枚举 - 决定触发记忆总结的阈值
 *
 * <p>启发式分类规则(SmartMemoryServiceImpl.classifyComplexity)：
 * 1. HIGH(高复杂度): 消息包含代码块(```)或单条长度>500字符 → 阈值20条
 *    - 原因：技术讨论需要更频繁的总结，保留代码细节
 *    - 适用场景：代码生成、Bug修复、技术方案讨论
 *
 * 2. MEDIUM(中复杂度): 消息包含"错误/异常/bug/fix/error"等关键词 → 阈值30条
 *    - 原因：问题排查场景需要中等频率总结
 *    - 适用场景：错误调试、问题诊断、异常处理
 *
 * 3. LOW(低复杂度): 默认情况 → 阈值40条
 *    - 原因：普通对话可以降低总结频率，减少AI调用成本
 *    - 适用场景：需求讨论、简单问答、确认性对话</p>
 *
 * <p>设计目的：
 * - 技术讨论需要更频繁的总结(保留代码细节)
 * - 普通对话可以降低总结频率(减少AI调用成本)
 * - 错误场景需要及时处理(提高总结频率)</p>
 *
 * @see com.yu.yuaicodemother.service.impl.SmartMemoryServiceImpl#classifyComplexity
 */
@Getter
public enum TopicComplexityEnum {

    LOW("低复杂度", "LOW", 40),
    MEDIUM("中复杂度", "MEDIUM", 30),
    HIGH("高复杂度", "HIGH", 20);

    private final String text;
    private final String value;
    private final int threshold;

    TopicComplexityEnum(String text, String value, int threshold) {
        this.text = text;
        this.value = value;
        this.threshold = threshold;
    }

    public static TopicComplexityEnum getEnumByValue(String value) {
        if (ObjUtil.isEmpty(value)) {
            return MEDIUM;
        }
        for (TopicComplexityEnum anEnum : TopicComplexityEnum.values()) {
            if (anEnum.value.equals(value)) {
                return anEnum;
            }
        }
        return MEDIUM;
    }
}
