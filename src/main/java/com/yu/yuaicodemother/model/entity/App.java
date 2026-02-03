package com.yu.yuaicodemother.model.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import com.mybatisflex.core.keygen.KeyGenerators;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 应用 实体类。
 *
 * @author 鱼🐟
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("app")
public class App implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * id
     */
    @Id(keyType = KeyType.Generator, value = KeyGenerators.snowFlakeId)
    private Long id;

    /**
     * 应用名称
     */
    @Column("appName")
    private String appName;

    /**
     * 应用封面
     */
    private String cover;

    /**
     * 应用初始化的 prompt
     */
    @Column("initPrompt")
    private String initPrompt;

    /**
     * 代码生成类型（枚举）
     */
    @Column("codeGenType")
    private String codeGenType;

    /**
     * 部署标识
     */
    @Column("deployKey")
    private String deployKey;

    /**
     * 部署时间
     */
    @Column("deployedTime")
    private LocalDateTime deployedTime;

    /**
     * 优先级
     */
    private Integer priority;

    /**
     * 创建用户id
     */
    @Column("userId")
    private Long userId;

    /**
     * 编辑时间
     */
    @Column("editTime")
    private LocalDateTime editTime;

    /**
     * 创建时间
     */
    @Column("createTime")
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @Column("updateTime")
    private LocalDateTime updateTime;

    /**
     * 可见范围(默认为true公开,false为私有)
     */
    @Column("visual_range")
    private boolean visualRange;

    /**
     * 当前版本号
     */
    @Column("current_version")
    private Integer currentVersion;

    /**
     * 部署状态（0=未部署，1=已上线，2=已下线）
     */
    @Column("deploy_status")
    private Integer deployStatus;

    /**
     * 生成状态（0=未开始，1=生成中，2=生成成功，3=生成失败）
     */
    @Column("gen_status")
    private Integer genStatus;

    /**
     * 精选状态：0-未申请, 1-申请中, 2-已精选, 3-已拒绝
     */
    @Column("featured_status")
    private Integer featuredStatus;

    /**
     * 用户个人优先级
     */
    @Column("user_priority")
    private Integer userPriority;

    /**
     * 是否删除
     */
    @Column(value = "isDelete", isLogicDelete = true)
    private Integer isDelete;

}
