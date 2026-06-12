package org.example.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "metric_audit_log")
public class MetricAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "metric_id")
    private Long metricId;

    @Column(name = "action", length = 50, nullable = false)
    private String action;

    @Column(name = "operator", length = 100)
    private String operator;

    @Column(name = "detail", columnDefinition = "TEXT")
    private String detail;

    @CreationTimestamp
    @Column(name = "created_time", updatable = false)
    private LocalDateTime createdTime;
}
