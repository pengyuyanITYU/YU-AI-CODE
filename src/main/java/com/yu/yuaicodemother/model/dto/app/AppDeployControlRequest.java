package com.yu.yuaicodemother.model.dto.app;

import lombok.Data;

import java.io.Serializable;

/**
 * 应用部署控制请求
 */
@Data
public class AppDeployControlRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 应用ID
     */
    private Long appId;

    /**
     * 部署状态（1=上线，2=下线）
     */
    private Integer deployStatus;
}
