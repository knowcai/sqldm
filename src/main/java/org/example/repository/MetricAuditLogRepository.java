package org.example.repository;

import org.example.entity.MetricAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MetricAuditLogRepository extends JpaRepository<MetricAuditLog, Long> {

    List<MetricAuditLog> findByMetricIdOrderByCreatedTimeDesc(Long metricId);
}
