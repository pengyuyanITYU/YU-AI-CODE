package com.yu.yuaicodemother.model.dto.app;

import lombok.Data;
import java.io.Serializable;

/**
 * 应用审核请求
 */
@Data
public class AppReviewRequest implements Serializable {

    /**
     * id
     */
    private Long id;

    /**
     * 精选状态：2-已精选, 3-已拒绝
     */
    private Integer featuredStatus;

    /**
     * 审核信息
     */
    private String reviewMessage;

    private static final long serialVersionUID = 1L;
}
