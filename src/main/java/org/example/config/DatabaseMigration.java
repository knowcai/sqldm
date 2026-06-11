package org.example.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DatabaseMigration implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        migrateMetricDefinition();
    }

    private void migrateMetricDefinition() {
        try {
            jdbcTemplate.execute("ALTER TABLE metric_definition ADD COLUMN IF NOT EXISTS metric_code VARCHAR(100)");
            jdbcTemplate.execute("ALTER TABLE metric_definition ADD COLUMN IF NOT EXISTS business_caliber TEXT");
            jdbcTemplate.execute("ALTER TABLE metric_definition ADD COLUMN IF NOT EXISTS stat_period VARCHAR(50)");
            jdbcTemplate.execute("ALTER TABLE metric_definition ADD COLUMN IF NOT EXISTS topic_id BIGINT");
            jdbcTemplate.execute("ALTER TABLE metric_definition ADD COLUMN IF NOT EXISTS topic_name VARCHAR(200)");
            jdbcTemplate.execute("ALTER TABLE metric_definition ADD COLUMN IF NOT EXISTS owner VARCHAR(100)");
            jdbcTemplate.execute("ALTER TABLE metric_definition ADD COLUMN IF NOT EXISTS status VARCHAR(50) DEFAULT 'ACTIVE'");
            jdbcTemplate.execute("ALTER TABLE metric_definition ADD COLUMN IF NOT EXISTS data_source VARCHAR(200)");
            jdbcTemplate.execute("ALTER TABLE metric_definition ADD COLUMN IF NOT EXISTS sql_template TEXT");
            jdbcTemplate.execute("ALTER TABLE metric_definition ADD COLUMN IF NOT EXISTS param_definition TEXT");

            relaxLegacyNotNull("main_table");
            relaxLegacyNotNull("metric_type");

            log.info("metric_definition 表结构迁移完成");
        } catch (Exception e) {
            log.warn("metric_definition 表结构迁移跳过或失败: {}", e.getMessage());
        }
    }

    private void relaxLegacyNotNull(String column) {
        try {
            jdbcTemplate.execute("ALTER TABLE metric_definition ALTER COLUMN " + column + " DROP NOT NULL");
            log.info("已移除 metric_definition.{} 的 NOT NULL 约束", column);
        } catch (Exception e) {
            log.debug("跳过 metric_definition.{} 约束调整: {}", column, e.getMessage());
        }
    }
}
