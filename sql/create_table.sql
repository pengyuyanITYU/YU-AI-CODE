# 数据库初始化
# @author <a href="https://github.com/liyupi">程序员鱼皮</a>
# @from <a href="https://codefather.cn">编程导航学习圈</a>

-- 创建库
create database if not exists yu_ai_code_mother;

-- 切换库
use yu_ai_code_mother;

-- 用户表
-- 以下是建表语句

-- 用户表
create table if not exists user
(
    id           bigint auto_increment comment 'id' primary key,
    userAccount  varchar(256)                           not null comment '账号',
    userPassword varchar(512)                           not null comment '密码',
    userName     varchar(256)                           null comment '用户昵称',
    userAvatar   varchar(1024)                          null comment '用户头像',
    userProfile  varchar(512)                           null comment '用户简介',
    userRole     varchar(256) default 'user'            not null comment '用户角色：user/admin',
    editTime     datetime     default CURRENT_TIMESTAMP not null comment '编辑时间',
    createTime   datetime     default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime   datetime     default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete     tinyint      default 0                 not null comment '是否删除',
    UNIQUE KEY uk_userAccount (userAccount),
    INDEX idx_userName (userName)
) comment '用户' collate = utf8mb4_unicode_ci;

-- 应用表
create table app
(
    id           bigint auto_increment comment 'id' primary key,
    appName      varchar(256)                       null comment '应用名称',
    cover        varchar(512)                       null comment '应用封面',
    initPrompt   text                               null comment '应用初始化的 prompt',
    codeGenType  varchar(64)                        null comment '代码生成类型（枚举）',
    deployKey    varchar(64)                        null comment '部署标识',
    deployedTime datetime                           null comment '部署时间',
    priority     int      default 0                 not null comment '优先级',
    userId       bigint                             not null comment '创建用户id',
    editTime     datetime default CURRENT_TIMESTAMP not null comment '编辑时间',
    createTime   datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime   datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete     tinyint  default 0                 not null comment '是否删除',
    UNIQUE KEY uk_deployKey (deployKey), -- 确保部署标识唯一
    INDEX idx_appName (appName),         -- 提升基于应用名称的查询性能
    INDEX idx_userId (userId)            -- 提升基于用户 ID 的查询性能
) comment '应用' collate = utf8mb4_unicode_ci;

-- 对话历史表
create table chat_history
(
    id          bigint auto_increment comment 'id' primary key,
    message     text                               not null comment '消息',
    messageType varchar(32)                        not null comment 'user/ai',
    appId       bigint                             not null comment '应用id',
    userId      bigint                             not null comment '创建用户id',
    createTime  datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime  datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete    tinyint  default 0                 not null comment '是否删除',
    INDEX idx_appId (appId),                       -- 提升基于应用的查询性能
    INDEX idx_createTime (createTime),             -- 提升基于时间的查询性能
    INDEX idx_appId_createTime (appId, createTime) -- 游标查询核心索引
) comment '对话历史' collate = utf8mb4_unicode_ci;

-- 扩展
-- 版本化管理
-- 应用主表 (保持现状，只需记录当前最新版本号)
ALTER TABLE app ADD COLUMN current_version INT DEFAULT 0;

-- 新增：应用版本历史表
CREATE TABLE app_versions (
                              id BIGINT PRIMARY KEY AUTO_INCREMENT,
                              app_id BIGINT NOT NULL,          -- 关联 app 表
                              version INT NOT NULL,            -- 版本号 (1, 2, 3...)
                              source_code_path VARCHAR(255),   -- 关键：该版本代码的物理存储路径
                              deploy_key VARCHAR(64),          -- 该版本的部署key
                              change_log TEXT,                 -- 你说的"distraction"，存变更描述
                              createTime  DATETIME DEFAULT CURRENT_TIMESTAMP,
                              INDEX idx_app_version (app_id, version)
);

-- 2026-02-02: 应用状态管理扩展
-- 添加部署状态字段（0=未部署，1=已上线，2=已下线）
ALTER TABLE app ADD COLUMN deploy_status INT DEFAULT 0 COMMENT '部署状态：0-未部署，1-已上线，2-已下线';

-- 添加生成状态字段（0=未开始，1=生成中，2=生成成功，3=生成失败）
ALTER TABLE app ADD COLUMN gen_status INT DEFAULT 0 COMMENT '生成状态：0-未开始，1-生成中，2-生成成功，3-生成失败';

-- 添加索引优化查询性能
CREATE INDEX idx_deploy_status ON app(deploy_status);
CREATE INDEX idx_gen_status ON app(gen_status);

-- 精选与置顶功能扩展
ALTER TABLE app ADD COLUMN featured_status INT DEFAULT 0 COMMENT '精选状态：0-未申请, 1-申请中, 2-已精选, 3-已拒绝';
ALTER TABLE app ADD COLUMN user_priority INT DEFAULT 0 COMMENT '用户个人优先级';
CREATE INDEX idx_featured_status ON app(featured_status);
CREATE INDEX idx_user_priority ON app(user_priority);

-- 2026-02-10: 统计应用对话轮次
ALTER TABLE app ADD COLUMN chat_count INT DEFAULT 0 COMMENT '对话轮次';

-- 2026-02-14: 智能记忆管理系统 - 对话记忆摘要表
-- 功能说明：存储AI对话的分层摘要，实现Token压缩(80%-90%)
-- 核心设计：三层结构 SHORT(短期) → MID(中期) → LONG(长期)
-- 使用场景：对话完成后异步生成摘要，加载时优先使用摘要+最近消息
CREATE TABLE chat_memory_summary (
    -- 主键字段
    id                BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID，雪花算法生成',

    -- 业务关联字段
    appId             BIGINT       NOT NULL COMMENT '应用ID，关联app表',
    userId            BIGINT       NOT NULL COMMENT '用户ID，摘要创建者',

    -- 摘要核心字段
    layer             VARCHAR(32)  NOT NULL COMMENT '摘要层级：SHORT(短期)/MID(中期)/LONG(长期)',
    summary           TEXT         NOT NULL COMMENT 'AI生成的摘要内容',

    -- 覆盖范围字段（用于增量总结）
    coveredFrom       BIGINT       NULL     COMMENT '覆盖起始消息ID（chat_history.id）',
    coveredTo         BIGINT       NULL     COMMENT '覆盖结束消息ID（chat_history.id）',
    coveredCount      INT          DEFAULT 0 COMMENT '本次摘要覆盖的原始消息数量',

    -- Token统计字段（用于压缩率计算）
    originalTokens    INT          DEFAULT 0 COMMENT '原始消息Token总数（JTokkit精确计算）',
    summaryTokens     INT          DEFAULT 0 COMMENT '生成摘要的Token数量',

    -- 复杂度与溯源字段
    topicComplexity   VARCHAR(16)  DEFAULT 'MEDIUM' COMMENT '主题复杂度：LOW(阈值40)/MEDIUM(30)/HIGH(20)',
    parentSummaryId   BIGINT       NULL     COMMENT '父摘要ID，记录MID/LONG由哪些下级摘要合并而来，用于溯源调试',

    -- 技术字段
    embedding         JSON         NULL     COMMENT '摘要语义向量（float[] JSON数组），用于相似度去重',
    version           INT          DEFAULT 0 COMMENT '乐观锁版本号，防止并发总结冲突',

    -- 时间字段
    createTime        DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    updateTime        DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    -- 软删除字段
    isDelete          TINYINT  DEFAULT 0 NOT NULL COMMENT '是否删除：0-未删除，1-已删除',

    -- 索引优化
    INDEX idx_app_layer (appId, layer) COMMENT '按应用和层级查询（最常用）',
    INDEX idx_app_createtime (appId, createTime) COMMENT '按应用和时间范围查询（用于清理旧数据）',
    INDEX idx_parent (parentSummaryId) COMMENT '按父摘要查询（用于溯源）'

) COMMENT '对话记忆摘要表 - 智能记忆管理系统核心表，支持分层摘要和Token压缩' COLLATE = utf8mb4_unicode_ci;
