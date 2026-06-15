-- ============================================================
-- 指标管理系统 sqldm v1.0 — 完整数据库初始化 (PostgreSQL)
-- ============================================================
--
-- 【用途】全新环境首次发布，一次性建库建表并初始化 root 账号。
--
-- 【执行示例】
--   psql -h 192.168.31.100 -U root -d vectordb -f sqldm_v1.0_full_init.sql
--
-- 【初始账号】
--   用户名: root
--   密码:   root
--   角色:   超级管理员 (SUPER_ADMIN)
--
-- 【说明】
--   1. 数据库仅通过本脚本手工初始化，应用启动不会自动建表。
--   2. 请先执行本脚本，再启动应用（JPA ddl-auto=validate 会校验表结构）。
--   3. 用户删除为物理删除；sys_user 无 is_deleted 字段。
--   4. 不含已废弃模块：标签、收藏、API 客户端、Webhook、调用日志。
--
-- ============================================================

-- ------------------------------------------------------------
-- 1. 用户
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS sys_user (
    id              BIGSERIAL PRIMARY KEY,
    username        VARCHAR(100)  NOT NULL,
    password        VARCHAR(255)  NOT NULL,
    real_name       VARCHAR(100),
    email           VARCHAR(200),
    role            VARCHAR(50)   NOT NULL DEFAULT 'USER',
    is_active       BOOLEAN       DEFAULT TRUE,
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    updated_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_sys_user_username UNIQUE (username)
);

COMMENT ON TABLE sys_user IS '系统用户';
COMMENT ON COLUMN sys_user.role IS 'SUPER_ADMIN=超级管理员, TOPIC_ADMIN=主题管理员, USER=普通用户';

-- ------------------------------------------------------------
-- 2. 主题
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS topic (
    id              BIGSERIAL PRIMARY KEY,
    topic_name      VARCHAR(200)  NOT NULL,
    topic_code      VARCHAR(100)  NOT NULL,
    description     TEXT,
    admin_id        BIGINT,
    created_by      VARCHAR(100),
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    updated_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    is_deleted      BOOLEAN       DEFAULT FALSE,
    CONSTRAINT uk_topic_code UNIQUE (topic_code)
);

COMMENT ON TABLE topic IS '业务主题域';

-- ------------------------------------------------------------
-- 3. 用户-主题关联
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS user_topic (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT        NOT NULL,
    topic_id        BIGINT        NOT NULL,
    role            VARCHAR(50)   NOT NULL DEFAULT 'MEMBER',
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_user_topic UNIQUE (user_id, topic_id)
);

CREATE INDEX IF NOT EXISTS idx_user_topic_user ON user_topic (user_id);
CREATE INDEX IF NOT EXISTS idx_user_topic_topic ON user_topic (topic_id);

COMMENT ON TABLE user_topic IS '用户与主题分配关系';
COMMENT ON COLUMN user_topic.role IS 'ADMIN=该主题审批员, MEMBER=普通成员';

-- ------------------------------------------------------------
-- 4. 指标定义
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS metric_definition (
    id                BIGSERIAL PRIMARY KEY,
    metric_name       VARCHAR(200)  NOT NULL,
    metric_code       VARCHAR(100),
    business_caliber  TEXT,
    stat_period       VARCHAR(50),
    topic_id          BIGINT,
    topic_name        VARCHAR(200),
    owner             VARCHAR(100),
    status            VARCHAR(50)   DEFAULT 'ACTIVE',
    data_source       VARCHAR(200),
    sql_template      TEXT,
    param_definition  TEXT,
    allowed_ips       TEXT,
    created_by        VARCHAR(100),
    created_time      TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    updated_by        VARCHAR(100),
    updated_time      TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    is_deleted        BOOLEAN       DEFAULT FALSE
);

CREATE INDEX IF NOT EXISTS idx_metric_definition_topic ON metric_definition (topic_id);
CREATE INDEX IF NOT EXISTS idx_metric_definition_code ON metric_definition (metric_code);
CREATE INDEX IF NOT EXISTS idx_metric_definition_status ON metric_definition (status);
CREATE INDEX IF NOT EXISTS idx_metric_definition_created ON metric_definition (created_time DESC);

COMMENT ON TABLE metric_definition IS '指标定义';

-- ------------------------------------------------------------
-- 5. 指标版本快照
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS metric_version (
    id              BIGSERIAL PRIMARY KEY,
    metric_id       BIGINT        NOT NULL,
    version_no      INTEGER       NOT NULL,
    snapshot        TEXT          NOT NULL,
    change_summary  VARCHAR(500),
    created_by      VARCHAR(100),
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_metric_version_metric ON metric_version (metric_id, version_no DESC);

-- ------------------------------------------------------------
-- 6. 指标操作审计
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS metric_audit_log (
    id              BIGSERIAL PRIMARY KEY,
    metric_id       BIGINT,
    action          VARCHAR(50)   NOT NULL,
    operator        VARCHAR(100),
    detail          TEXT,
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_metric_audit_metric ON metric_audit_log (metric_id, created_time DESC);

-- ------------------------------------------------------------
-- 7. 指标审批申请
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS metric_approval_request (
    id              BIGSERIAL PRIMARY KEY,
    metric_id       BIGINT        NOT NULL,
    topic_id        BIGINT        NOT NULL,
    request_type    VARCHAR(20)   NOT NULL,
    payload         TEXT          NOT NULL,
    status          VARCHAR(20)   NOT NULL DEFAULT 'PENDING',
    submitted_by    VARCHAR(100),
    reviewed_by     VARCHAR(100),
    review_comment  VARCHAR(500),
    created_time    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    reviewed_time   TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_approval_topic_status ON metric_approval_request (topic_id, status);
CREATE INDEX IF NOT EXISTS idx_approval_metric_status ON metric_approval_request (metric_id, status);

-- ------------------------------------------------------------
-- 8. 最近访问
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS metric_recent_access (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT        NOT NULL,
    metric_id       BIGINT        NOT NULL,
    accessed_time   TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_recent_access_user_metric UNIQUE (user_id, metric_id)
);

CREATE INDEX IF NOT EXISTS idx_recent_access_user ON metric_recent_access (user_id, accessed_time DESC);

-- ------------------------------------------------------------
-- 9. 初始超级管理员 root / root
-- ------------------------------------------------------------
INSERT INTO sys_user (username, password, real_name, role, is_active)
SELECT 'root',
       '$2a$10$sG8rmhe83D9B5XZVRwtSsuXyE2FzI9I1q20lu1XrDiEsDO92eZU1C',
       '超级管理员',
       'SUPER_ADMIN',
       TRUE
WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE username = 'root');
