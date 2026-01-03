package com.yu.yuaicodemother.model.enums;

import cn.hutool.core.util.ObjUtil;
import dev.langchain4j.model.output.structured.Description;
import lombok.Getter;

@Getter
public enum CodeGenTypeEnum {

    @Description("适合简单的静态页面，单个 HTML 文件，包含内联 CSS 和 JS")
    HTML("原生HTML模式", "html"),
    @Description("适合简单的多文件静态页面，分离 HTML、CSS、JS 代码")
    MULTI_FILE("原生多文件模式", "multi_file"),
    @Description("适合复杂的现代化前端项目，涉及多页面、复杂交互、数据管理等")
    VUE_PROJECT("Vue工程模式", "vue_project");


    private final String text;
    private final String value;

    CodeGenTypeEnum(String text, String value) {
        this.text = text;
        this.value = value;
    }

    /**
     * 根据 value 获取枚举
     *
     * @param value 枚举值的value
     * @return 枚举值
     */
    public static CodeGenTypeEnum getEnumByValue(String value) {
        if (ObjUtil.isEmpty(value)) {
            return null;
        }
        for (CodeGenTypeEnum anEnum : CodeGenTypeEnum.values()) {
            if (anEnum.value.equals(value)) {
                return anEnum;
            }
        }
        return null;
    }
}
