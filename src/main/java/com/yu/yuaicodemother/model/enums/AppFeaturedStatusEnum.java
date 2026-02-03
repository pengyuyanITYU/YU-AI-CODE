package com.yu.yuaicodemother.model.enums;

import lombok.Getter;

/**
 * 应用精选状态枚举
 */
@Getter
public enum AppFeaturedStatusEnum {

    NOT_APPLIED(0, "未申请"),
    PENDING(1, "申请中"),
    FEATURED(2, "已精选"),
    REJECTED(3, "已拒绝");

    private final int value;
    private final String text;

    AppFeaturedStatusEnum(int value, String text) {
        this.value = value;
        this.text = text;
    }

    /**
     * 根据 value 获取枚举
     *
     * @param value
     * @return
     */
    public static AppFeaturedStatusEnum getEnumByValue(Integer value) {
        if (value == null) {
            return null;
        }
        for (AppFeaturedStatusEnum anEnum : AppFeaturedStatusEnum.values()) {
            if (anEnum.value == value) {
                return anEnum;
            }
        }
        return null;
    }
}
