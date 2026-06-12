-- 兼容旧版 schema（幂等）
ALTER TABLE metric_definition ADD COLUMN IF NOT EXISTS metric_code VARCHAR(100);
ALTER TABLE metric_definition ADD COLUMN IF NOT EXISTS business_caliber TEXT;
ALTER TABLE metric_definition ADD COLUMN IF NOT EXISTS stat_period VARCHAR(50);
ALTER TABLE metric_definition ADD COLUMN IF NOT EXISTS topic_id BIGINT;
ALTER TABLE metric_definition ADD COLUMN IF NOT EXISTS topic_name VARCHAR(200);
ALTER TABLE metric_definition ADD COLUMN IF NOT EXISTS owner VARCHAR(100);
ALTER TABLE metric_definition ADD COLUMN IF NOT EXISTS status VARCHAR(50) DEFAULT 'ACTIVE';
ALTER TABLE metric_definition ADD COLUMN IF NOT EXISTS data_source VARCHAR(200);
ALTER TABLE metric_definition ADD COLUMN IF NOT EXISTS sql_template TEXT;
ALTER TABLE metric_definition ADD COLUMN IF NOT EXISTS param_definition TEXT;
ALTER TABLE metric_definition ADD COLUMN IF NOT EXISTS allowed_ips TEXT;

DO $$ BEGIN
    ALTER TABLE metric_definition ALTER COLUMN main_table DROP NOT NULL;
EXCEPTION WHEN undefined_column OR undefined_table THEN NULL;
END $$;
DO $$ BEGIN
    ALTER TABLE metric_definition ALTER COLUMN metric_type DROP NOT NULL;
EXCEPTION WHEN undefined_column OR undefined_table THEN NULL;
END $$;

CREATE TABLE IF NOT EXISTS metric_version (
    id BIGSERIAL PRIMARY KEY,
    metric_id BIGINT NOT NULL,
    version_no INTEGER NOT NULL,
    snapshot TEXT NOT NULL,
    change_summary VARCHAR(500),
    created_by VARCHAR(100),
    created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS metric_audit_log (
    id BIGSERIAL PRIMARY KEY,
    metric_id BIGINT,
    action VARCHAR(50) NOT NULL,
    operator VARCHAR(100),
    detail TEXT,
    created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS api_client (
    id SERIAL PRIMARY KEY,
    client_name VARCHAR(100) NOT NULL,
    api_key VARCHAR(64) NOT NULL UNIQUE,
    allowed_ips TEXT,
    is_active BOOLEAN DEFAULT TRUE,
    description VARCHAR(500),
    created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
