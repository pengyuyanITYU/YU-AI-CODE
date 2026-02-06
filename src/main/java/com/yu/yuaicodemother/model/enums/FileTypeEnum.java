package com.yu.yuaicodemother.model.enums;

import lombok.Getter;

@Getter
public enum FileTypeEnum {

    IMAGE("image", "图片"),
    DOCUMENT("document", "文档"),
    TEXT("text", "文本");

    private final String value;
    private final String description;

    FileTypeEnum(String value, String description) {
        this.value = value;
        this.description = description;
    }

    public static FileTypeEnum getByValue(String value) {
        for (FileTypeEnum type : values()) {
            if (type.value.equals(value)) {
                return type;
            }
        }
        return null;
    }
}
