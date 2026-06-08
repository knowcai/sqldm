-- 指标管理系统建表脚本
-- PostgreSQL 16

-- 创建指标定义表
CREATE TABLE IF NOT EXISTS metric_definition (
    id BIGSERIAL PRIMARY KEY,
    metric_name VARCHAR(200) NOT NULL COMMENT '指标名称',
    metric_type VARCHAR(50) NOT NULL COMMENT '指标类型：STATISTICS-统计，DETAIL-明细',
    main_table VARCHAR(200) NOT NULL COMMENT '主表名',
    join_tables TEXT COMMENT '关联表（JSON格式存储多个关联表信息）',
    join_fields TEXT COMMENT '关联字段（JSON格式存储）',
    dimension_fields TEXT COMMENT '维度字段（逗号分隔）',
    filter_fields TEXT COMMENT '过滤字段（JSON格式存储）',
    description TEXT COMMENT '指标注释/描述',
    created_by VARCHAR(100) DEFAULT 'system' COMMENT '创建人',
    created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_by VARCHAR(100) DEFAULT 'system' COMMENT '更新人',
    updated_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    is_deleted BOOLEAN DEFAULT FALSE COMMENT '是否删除',
    CONSTRAINT uk_metric_name UNIQUE (metric_name)
);

-- 创建索引
CREATE INDEX idx_metric_name ON metric_definition(metric_name);
CREATE INDEX idx_metric_type ON metric_definition(metric_type);
CREATE INDEX idx_created_time ON metric_definition(created_time);

-- 添加表注释
COMMENT ON TABLE metric_definition IS '指标定义表';
COMMENT ON COLUMN metric_definition.id IS '主键ID';
COMMENT ON COLUMN metric_definition.metric_name IS '指标名称';
COMMENT ON COLUMN metric_definition.metric_type IS '指标类型：STATISTICS-统计，DETAIL-明细';
COMMENT ON COLUMN metric_definition.main_table IS '主表名';
COMMENT ON COLUMN metric_definition.join_tables IS '关联表（JSON格式）';
COMMENT ON COLUMN metric_definition.join_fields IS '关联字段（JSON格式）';
COMMENT ON COLUMN metric_definition.dimension_fields IS '维度字段（逗号分隔）';
COMMENT ON COLUMN metric_definition.filter_fields IS '过滤字段（JSON格式）';
COMMENT ON COLUMN metric_definition.description IS '指标注释/描述';
COMMENT ON COLUMN metric_definition.created_by IS '创建人';
COMMENT ON COLUMN metric_definition.created_time IS '创建时间';
COMMENT ON COLUMN metric_definition.updated_by IS '更新人';
COMMENT ON COLUMN metric_definition.updated_time IS '更新时间';
COMMENT ON COLUMN metric_definition.is_deleted IS '是否删除';

-- 插入示例数据
INSERT INTO metric_definition (metric_name, metric_type, main_table, join_tables, join_fields, dimension_fields, filter_fields, description)
VALUES 
('用户注册统计', 'STATISTICS', 'users', '[{"table":"orders","alias":"o"}]', '[{"mainField":"user_id","joinField":"user_id"}]', 'register_date,city', '[{"field":"status","operator":"=","value":"active"}]', '统计每日各城市活跃用户的注册数量'),
('订单明细查询', 'DETAIL', 'orders', '[{"table":"users","alias":"u"},{"table":"products","alias":"p"}]', '[{"mainField":"user_id","joinField":"id"},{"mainField":"product_id","joinField":"id"}]', 'order_date,order_status', '[{"field":"amount","operator":">","value":"100"}]', '查询金额大于100的订单详细信息');
