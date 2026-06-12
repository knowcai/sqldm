package org.example.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "metric_version")
public class MetricVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "metric_id", nullable = false)
    private Long metricId;

    @Column(name = "version_no", nullable = false)
    private Integer versionNo;

    /** 指标快照 JSON */
    @Column(name = "snapshot", columnDefinition = "TEXT", nullable = false)
    private String snapshot;

    @Column(name = "change_summary", length = 500)
    private String changeSummary;

    @Column(name = "created_by", length = 100)
    private String createdBy;

    @CreationTimestamp
    @Column(name = "created_time", updatable = false)
    private LocalDateTime createdTime;
}
