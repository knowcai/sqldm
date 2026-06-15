package org.sqldm.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "metric_approval_request")
public class MetricApprovalRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "metric_id", nullable = false)
    private Long metricId;

    @Column(name = "topic_id", nullable = false)
    private Long topicId;

    /** CREATE / UPDATE */
    @Column(name = "request_type", nullable = false, length = 20)
    private String requestType;

    @Column(name = "payload", nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Column(name = "status", nullable = false, length = 20)
    private String status = "PENDING";

    @Column(name = "submitted_by", length = 100)
    private String submittedBy;

    @Column(name = "reviewed_by", length = 100)
    private String reviewedBy;

    @Column(name = "review_comment", length = 500)
    private String reviewComment;

    @CreationTimestamp
    @Column(name = "created_time", updatable = false)
    private LocalDateTime createdTime;

    @Column(name = "reviewed_time")
    private LocalDateTime reviewedTime;
}
