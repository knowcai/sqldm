package org.example.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
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

    @Column(name = "metric_code", length = 100)
    private String metricCode;

    @Column(name = "business_caliber", columnDefinition = "TEXT")
    private String businessCaliber;

    /** DAY-日, MONTH-月, WEEK-周, REALTIME-实时 */
    @Column(name = "stat_period", length = 50)
    private String statPeriod;

    @Column(name = "topic_id")
    private Long topicId;

    @Column(name = "topic_name", length = 200)
    private String topicName;

    @Column(name = "owner", length = 100)
    private String owner;

    /** DRAFT-草稿, PENDING_APPROVAL-待审批, ACTIVE-启用, DISABLED-停用, REJECTED-已驳回 */
    @Column(name = "status", length = 50)
    private String status = "ACTIVE";

    @Column(name = "data_source", length = 200)
    private String dataSource;

    @Column(name = "sql_template", columnDefinition = "TEXT")
    private String sqlTemplate;

    /** JSON 格式参数定义 */
    @Column(name = "param_definition", columnDefinition = "TEXT")
    private String paramDefinition;

    /** JSON 格式允许访问 Open API 的 IP 列表，空表示不限制 */
    @Column(name = "allowed_ips", columnDefinition = "TEXT")
    private String allowedIps;

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

    @Transient
    private java.util.List<String> tags;

    @Transient
    private Boolean favorited;

    @Transient
    private Boolean hasPendingUpdate;

    @Transient
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private java.util.List<String> duplicateWarnings;
}
