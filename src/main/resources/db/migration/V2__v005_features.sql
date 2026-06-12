-- v0.05: 标签、收藏、最近访问、调用日志、Webhook
CREATE TABLE IF NOT EXISTS metric_tag (
    id SERIAL PRIMARY KEY,
    tag_name VARCHAR(50) NOT NULL UNIQUE,
    created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS metric_tag_rel (
    id BIGSERIAL PRIMARY KEY,
    metric_id BIGINT NOT NULL,
    tag_id BIGINT NOT NULL,
    UNIQUE (metric_id, tag_id)
);

CREATE TABLE IF NOT EXISTS metric_favorite (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    metric_id BIGINT NOT NULL,
    created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (user_id, metric_id)
);

CREATE TABLE IF NOT EXISTS metric_recent_access (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    metric_id BIGINT NOT NULL,
    accessed_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (user_id, metric_id)
);

CREATE TABLE IF NOT EXISTS api_access_log (
    id BIGSERIAL PRIMARY KEY,
    api_key_hint VARCHAR(16),
    client_ip VARCHAR(64),
    metric_code VARCHAR(100),
    metric_id BIGINT,
    version_no INTEGER,
    success BOOLEAN NOT NULL,
    error_message VARCHAR(500),
    elapsed_ms BIGINT,
    created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS webhook_config (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    url VARCHAR(500) NOT NULL,
    secret VARCHAR(200),
    events TEXT NOT NULL,
    is_active BOOLEAN DEFAULT TRUE,
    created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_api_access_log_time ON api_access_log (created_time DESC);
CREATE INDEX IF NOT EXISTS idx_metric_tag_rel_metric ON metric_tag_rel (metric_id);
CREATE INDEX IF NOT EXISTS idx_metric_favorite_user ON metric_favorite (user_id);
