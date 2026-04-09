-- 创建库
create database if not exists ping_picture;

-- 切换库
use ping_picture;

-- 图片 AI 调用 token 统计表
create table if not exists picture_audit_token_stats
(
    id            bigint auto_increment comment '主键id' primary key,
    pictureId     bigint         not null comment '图片id',
    userId        bigint         not null comment '用户id',
    spaceId       bigint         null comment '空间id（公共图库为null）',
    taskType      varchar(32)    not null default 'review' comment '任务类型：review/outpainting',
    modelName     varchar(64)    not null comment '模型名称',
    inputTokens   bigint         null comment '输入token数',
    outputTokens  bigint         null comment '输出token数',
    totalTokens   bigint         null comment '总token数',
    cost          decimal(10, 6) null comment '调用成本（元），精确到微元',
    reviewStatus  int            not null default 0 comment '审核状态：0-待审核; 1-通过; 2-拒绝',
    reviewMessage varchar(512)   null comment '审核信息',
    costTime      bigint         null comment '调用耗时（毫秒）',
    errorMsg      varchar(512)   null comment '失败时的错误信息',
    createTime    datetime       not null default current_timestamp comment '创建时间',
    -- 索引设计
    index idx_picture_id (pictureId),       -- 提升按图片查询的性能
    index idx_user_id (userId),             -- 提升按用户查询的性能
    index idx_space_id (spaceId),           -- 提升按空间查询的性能
    index idx_create_time (createTime),     -- 提升按时间范围查询的性能
    index idx_review_status (reviewStatus), -- 提升按审核结果查询的性能
    index idx_cost (cost)                   -- 提升按成本排序查询的性能
) comment '图片 AI 调用 token 统计表' collate = utf8mb4_unicode_ci;

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

-- 图片表
create table if not exists picture
(
    id           bigint auto_increment comment 'id' primary key,
    url          varchar(512)                       not null comment '图片 url',
    name         varchar(128)                       not null comment '图片名称',
    introduction varchar(512)                       null comment '简介',
    category     varchar(64)                        null comment '分类',
    tags         varchar(512)                       null comment '标签（JSON 数组）',
    picSize      bigint                             null comment '图片体积',
    picWidth     int                                null comment '图片宽度',
    picHeight    int                                null comment '图片高度',
    picScale     double                             null comment '图片宽高比例',
    picFormat    varchar(32)                        null comment '图片格式',
    userId       bigint                             not null comment '创建用户 id',
    createTime   datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    editTime     datetime default CURRENT_TIMESTAMP not null comment '编辑时间',
    updateTime   datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete     tinyint  default 0                 not null comment '是否删除',
    INDEX idx_name (name),                 -- 提升基于图片名称的查询性能
    INDEX idx_introduction (introduction), -- 用于模糊搜索图片简介
    INDEX idx_category (category),         -- 提升基于分类的查询性能
    INDEX idx_tags (tags),                 -- 提升基于标签的查询性能
    INDEX idx_userId (userId)              -- 提升基于用户 ID 的查询性能
) comment '图片' collate = utf8mb4_unicode_ci;

ALTER TABLE picture
    -- 添加新列
    ADD COLUMN reviewStatus  INT DEFAULT 0 NOT NULL COMMENT '审核状态：0-待审核; 1-通过; 2-拒绝',
    ADD COLUMN reviewMessage VARCHAR(512)  NULL COMMENT '审核信息',
    ADD COLUMN reviewerId    BIGINT        NULL COMMENT '审核人 ID',
    ADD COLUMN reviewTime    DATETIME      NULL COMMENT '审核时间';

-- 创建基于 reviewStatus 列的索引
CREATE INDEX idx_reviewStatus ON picture (reviewStatus);

ALTER TABLE picture
    -- 添加新列
    ADD COLUMN thumbnailUrl varchar(512) NULL COMMENT '缩略图 url';

ALTER TABLE picture
    -- 添加新列
    ADD COLUMN originalUrl varchar(512) NULL COMMENT '初始原图 url';

-- 空间表
create table if not exists space
(
    id         bigint auto_increment comment 'id' primary key,
    spaceName  varchar(128)                       null comment '空间名称',
    spaceLevel int      default 0                 null comment '空间级别：0-普通版 1-专业版 2-旗舰版',
    maxSize    bigint   default 0                 null comment '空间图片的最大总大小',
    maxCount   bigint   default 0                 null comment '空间图片的最大数量',
    totalSize  bigint   default 0                 null comment '当前空间下图片的总大小',
    totalCount bigint   default 0                 null comment '当前空间下的图片数量',
    userId     bigint                             not null comment '创建用户 id',
    createTime datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    editTime   datetime default CURRENT_TIMESTAMP not null comment '编辑时间',
    updateTime datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete   tinyint  default 0                 not null comment '是否删除',
    -- 索引设计
    index idx_userId (userId),        -- 提升基于用户的查询效率
    index idx_spaceName (spaceName),  -- 提升基于空间名称的查询效率
    index idx_spaceLevel (spaceLevel) -- 提升按空间级别查询的效率
) comment '空间' collate = utf8mb4_unicode_ci;

-- 添加新列
ALTER TABLE picture
    ADD COLUMN spaceId  bigint  null comment '空间 id（为空表示公共空间）';

-- 创建索引
CREATE INDEX idx_spaceId ON picture (spaceId);

-- 添加新列 - 根据颜色搜图
ALTER TABLE picture
    ADD COLUMN picColor varchar(16) null comment '图片主色调';


ALTER TABLE space
    ADD COLUMN spaceType int default 0 not null comment '空间类型：0-私有 1-团队';

CREATE INDEX idx_spaceType ON space (spaceType);

-- 空间成员表
create table if not exists space_user
(
    id         bigint auto_increment comment 'id' primary key,
    spaceId    bigint                                 not null comment '空间 id',
    userId     bigint                                 not null comment '用户 id',
    spaceRole  varchar(128) default 'viewer'          null comment '空间角色：viewer/editor/admin',
    createTime datetime     default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime datetime     default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    -- 索引设计
    UNIQUE KEY uk_spaceId_userId (spaceId, userId), -- 唯一索引，用户在一个空间中只能有一个角色
    INDEX idx_spaceId (spaceId),                    -- 提升按空间查询的性能
    INDEX idx_userId (userId)                       -- 提升按用户查询的性能
) comment '空间用户关联' collate = utf8mb4_unicode_ci;


-- 为 user 表添加会员相关字段
ALTER TABLE user
    ADD COLUMN vipExpireTime datetime     NULL COMMENT '会员过期时间',
    ADD COLUMN vipCode       varchar(128) NULL COMMENT '会员兑换码',
    ADD COLUMN vipNumber     bigint       NULL COMMENT '会员编号';

-- 会员兑换码表
create table if not exists vip_exchange_code
(
    id           bigint auto_increment comment 'id' primary key,
    exchangeCode varchar(128)                          not null comment '兑换码',
    hasUsed      tinyint     default 0                 not null comment '是否已兑换',
    source       VARCHAR(50) DEFAULT NULL COMMENT '来源活动/渠道',
    creator      VARCHAR(64) DEFAULT NULL COMMENT '创建人',
    createTime   datetime    default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime   datetime    default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    -- 索引设计
    UNIQUE KEY uk_exchangeCode (exchangeCode) -- 唯一索引，兑换码全局唯一
    ) comment '会员兑换码' collate = utf8mb4_unicode_ci;
-- 插入20条随机格式的会员兑换码
INSERT INTO vip_exchange_code (exchangeCode, hasUsed, source, creator)
VALUES
-- 未使用的兑换码
('F4gH5jK8', 0, '春节活动', 'admin'),
('R2tY7uI9', 0, '春节活动', 'admin'),
('W3qE6rT4', 0, '新用户注册', 'system'),
('B5nM8lP2', 0, '新用户注册', 'system'),
('X1zC4vB7', 0, '新用户注册', 'system'),
('J9kL0oP3', 0, '618大促', '运营小张'),
('Q2wS5xE6', 0, '618大促', '运营小张'),
('A7sD8fG1', 0, '618大促', '运营小张'),
('Z4xR6tY9', 0, '邀请有礼', 'system'),
('C3vB5nM2', 0, '邀请有礼', 'system'),
('E7rT8zU1', 0, '邀请有礼', 'system'),
('L0oP9iK4', 0, '老用户回馈', '运营小李'),
('U2yH5jN7', 0, '老用户回馈', '运营小李'),
('I8kM3lO6', 0, '新用户注册', 'system'),
('P5aS9dF2', 0, '618大促', '运营小张'),

-- 已使用的兑换码
('G1hJ4kL7', 1, '春节活动', 'admin'),
('V6bN8mQ3', 1, '春节活动', 'admin'),
('T9rE2wR5', 1, '新用户注册', 'system'),
('Y4uI7oP1', 1, '邀请有礼', 'system'),
('N3mM6lL8', 1, '老用户回馈', '运营小李');