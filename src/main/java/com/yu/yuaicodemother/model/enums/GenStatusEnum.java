package com.yu.yuaicodemother.model.enums;

import lombok.Getter;

/**
 * 应用生成状态枚举
 */
@Getter
public enum GenStatusEnum {

    NOT_GENERATED(0, "未生成"),
    GENERATING(1, "生成中"),
    COMPLETED(2, "生成完成"),
    FAILED(3, "生成失败"),
    INTERRUPTED(4, "已中断");

    private final int value;
    private final String text;

    GenStatusEnum(int value, String text) {
        this.value = value;
        this.text = text;
    }

    /**
     * 根据 value 获取枚举
     *
     * @param value
     * @return
     */
    public static GenStatusEnum getEnumByValue(Integer value) {
        if (value == null) {
            return null;
        }
        for (GenStatusEnum anEnum : GenStatusEnum.values()) {
            if (anEnum.value == value) {
                return anEnum;
            }
        }
        return null;
    }
}
