package com.yu.yuaicodemother.model.enums;

import lombok.Getter;

/**
 * 应用生成状态枚举
 */
@Getter
public enum AppGenStatusEnum {

    NOT_STARTED(0, "未开始"),
    GENERATING(1, "生成中"),
    GENERATED_SUCCESS(2, "生成成功"),
    GENERATED_FAILED(3, "生成失败");

    private final int value;
    private final String text;

    AppGenStatusEnum(int value, String text) {
        this.value = value;
        this.text = text;
    }

    /**
     * 根据 value 获取枚举
     *
     * @param value
     * @return
     */
    public static AppGenStatusEnum getEnumByValue(Integer value) {
        if (value == null) {
            return null;
        }
        for (AppGenStatusEnum anEnum : AppGenStatusEnum.values()) {
            if (anEnum.value == value) {
                return anEnum;
            }
        }
        return null;
    }
}
