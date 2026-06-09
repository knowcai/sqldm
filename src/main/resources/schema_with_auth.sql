-- ============================================
-- 指标管理系统 - 数据库初始化脚本
-- 包含用户、主题和权限管理的表结构
-- ============================================

-- 1. 主题表
CREATE TABLE IF NOT EXISTS topic (
    id SERIAL PRIMARY KEY,
    topic_name VARCHAR(200) NOT NULL,
    topic_code VARCHAR(100) NOT NULL,
    description TEXT,
    admin_id BIGINT,
    created_by VARCHAR(100),
    created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_deleted BOOLEAN DEFAULT FALSE,
    CONSTRAINT uk_topic_code UNIQUE (topic_code)
);

-- 2. 用户表
CREATE TABLE IF NOT EXISTS sys_user (
    id SERIAL PRIMARY KEY,
    username VARCHAR(100) NOT NULL,
    password VARCHAR(255) NOT NULL,
    real_name VARCHAR(100),
    email VARCHAR(200),
    role VARCHAR(50) NOT NULL DEFAULT 'USER', -- SUPER_ADMIN, TOPIC_ADMIN, USER
    is_active BOOLEAN DEFAULT TRUE,
    created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_deleted BOOLEAN DEFAULT FALSE,
    CONSTRAINT uk_username UNIQUE (username)
);

-- 3. 用户-主题关联表
CREATE TABLE IF NOT EXISTS user_topic (
    id SERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    topic_id BIGINT NOT NULL,
    role VARCHAR(50) NOT NULL DEFAULT 'MEMBER', -- ADMIN, MEMBER
    created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_user_topic UNIQUE (user_id, topic_id)
);

-- 4. 修改指标表,增加主题关联
ALTER TABLE metric_definition ADD COLUMN IF NOT EXISTS topic_id BIGINT;
ALTER TABLE metric_definition ADD COLUMN IF NOT EXISTS topic_name VARCHAR(200);

-- ============================================
-- 初始数据说明
-- ============================================
-- 由于BCrypt密码哈希值每次生成都不同,需要在应用启动后通过代码初始化
-- 请按照以下步骤操作:
--
-- 1. 启动应用后,访问 http://localhost:8080/init-users
-- 2. 该接口会自动创建以下默认用户:
--    - 超级管理员: root / root
--    - 主题管理员1: admin1 / admin1
--    - 主题管理员2: admin2 / admin2
--    - 普通用户1: user1 / user1
--    - 普通用户2: user2 / user2
--
-- 3. 或者手动通过管理员页面创建用户
-- ============================================

-- 注释: 
-- 1. SUPER_ADMIN: 超级管理员,可以管理所有用户和主题
-- 2. TOPIC_ADMIN: 主题管理员,可以创建和维护主题,管理主题下的指标
-- 3. USER: 普通用户,只有查询权限
--
-- 4. 在user_topic表中:
--    - ADMIN: 主题管理员(可以管理该主题下的指标)
--    - MEMBER: 主题成员(只能查询该主题下的指标)
