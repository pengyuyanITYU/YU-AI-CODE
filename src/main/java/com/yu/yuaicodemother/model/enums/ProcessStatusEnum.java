package com.yu.yuaicodemother.model.enums;

import lombok.Getter;

@Getter
public enum ProcessStatusEnum {

    SUCCESS("success", "处理成功"),
    FAILED("failed", "处理失败"),
    EMPTY("empty", "内容为空"),
    OCR_REQUIRED("ocr_required", "需要OCR识别");

    private final String value;
    private final String description;

    ProcessStatusEnum(String value, String description) {
        this.value = value;
        this.description = description;
    }

    public static ProcessStatusEnum getByValue(String value) {
        for (ProcessStatusEnum status : values()) {
            if (status.value.equals(value)) {
                return status;
            }
        }
        return null;
    }
}
