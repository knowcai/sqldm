package org.example.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "metric_definition")
public class MetricDefinition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "metric_name", nullable = false, length = 200)
    private String metricName;

    @Column(name = "metric_type", nullable = false, length = 50)
    private String metricType; // STATISTICS-统计, DETAIL-明细
    
    @Column(name = "topic_id")
    private Long topicId;
    
    @Column(name = "topic_name", length = 200)
    private String topicName;

    @Column(name = "main_table", nullable = false, length = 200)
    private String mainTable;

    @Column(name = "join_tables", columnDefinition = "TEXT")
    private String joinTables; // JSON格式存储关联表信息

    @Column(name = "join_fields", columnDefinition = "TEXT")
    private String joinFields; // JSON格式存储关联字段

    @Column(name = "dimension_fields", columnDefinition = "TEXT")
    private String dimensionFields; // 逗号分隔的维度字段

    @Column(name = "filter_fields", columnDefinition = "TEXT")
    private String filterFields; // JSON格式存储过滤字段

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "created_by", length = 100)
    private String createdBy;

    @CreationTimestamp
    @Column(name = "created_time", updatable = false)
    private LocalDateTime createdTime;

    @Column(name = "updated_by", length = 100)
    private String updatedBy;

    @UpdateTimestamp
    @Column(name = "updated_time")
    private LocalDateTime updatedTime;

    @Column(name = "is_deleted")
    private Boolean isDeleted = false;
}
