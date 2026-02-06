package com.yu.yuaicodemother.model.enums;

/**
 * 应用部署状态枚举
 */
public enum AppDeployStatusEnum {

    NOT_DEPLOYED(0, "未部署"),
    ONLINE(1, "已上线"),
    OFFLINE(2, "已下线");

    private final int value;
    private final String text;

    AppDeployStatusEnum(int value, String text) {
        this.value = value;
        this.text = text;
    }

    public int getValue() {
        return value;
    }

    public String getText() {
        return text;
    }

    /**
     * 根据 value 获取枚举
     *
     * @param value
     * @return
     */
    public static AppDeployStatusEnum getEnumByValue(Integer value) {
        if (value == null) {
            return null;
        }
        for (AppDeployStatusEnum anEnum : AppDeployStatusEnum.values()) {
            if (anEnum.value == value) {
                return anEnum;
            }
        }
        return null;
    }
}
