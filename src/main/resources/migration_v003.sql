-- v0.03 迁移：对齐 metric_definition 表结构与当前业务模型
-- 解决保存时报错：null value in column "main_table" violates not-null constraint

-- 新增 v0.02/v0.03 业务字段
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

-- 放宽旧版字段约束（v0.01 遗留，应用已不再使用）
ALTER TABLE metric_definition ALTER COLUMN main_table DROP NOT NULL;
ALTER TABLE metric_definition ALTER COLUMN metric_type DROP NOT NULL;
