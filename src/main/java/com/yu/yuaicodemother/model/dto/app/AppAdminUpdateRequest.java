package com.yu.yuaicodemother.model.dto.app;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class AppAdminUpdateRequest implements Serializable {

    /**
     * id
     */
    private Long id;

    /**
     * 应用名称
     */
    private String appName;

    /**
     * 应用封面
     */
    private String cover;

    /**
     * 优先级
     */
    private Integer priority;

    /**
     * 精选状态
     */
    private Integer featuredStatus;

    @Serial
    private static final long serialVersionUID = 1L;
}
