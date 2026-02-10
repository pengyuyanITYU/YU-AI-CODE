package com.yu.yuaicodemother.model.vo.app;

import com.yu.yuaicodemother.model.vo.user.UserVO;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class AppVO implements Serializable {

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
     * 应用初始化的 prompt
     */
    private String initPrompt;

    /**
     * 代码生成类型（枚举）
     */
    private String codeGenType;

    /**
     * 部署标识
     */
    private String deployKey;

    /**
     * 部署时间
     */
    private LocalDateTime deployedTime;

    /**
     * 优先级
     */
    private Integer priority;

    /**
     * 创建用户id
     */
    private Long userId;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

    /**
     * 创建用户信息
     */
    private UserVO user;

    /**
     * 可见范围
     */
    private boolean visualRange;

    /**
     * 部署状态（0=未部署，1=已上线，2=已下线）
     */
    private Integer deployStatus;

    /**
     * 生成状态（0=未开始，1=生成中，2=生成成功，3=生成失败）
     */
    private Integer genStatus;

    /**
     * 精选状态
     */
    private Integer featuredStatus;

    /**
     * 用户个人优先级
     */
    private Integer userPriority;

    /**
     * 审核信息
     */
    private String reviewMessage;

    /**
     * 对话轮次
     */
    private Integer chatCount;

    @Serial


    private static final long serialVersionUID = 1L;
}
