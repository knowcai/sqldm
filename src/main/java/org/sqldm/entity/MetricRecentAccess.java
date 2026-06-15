package org.sqldm.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "metric_recent_access", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "metric_id"}))
public class MetricRecentAccess {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "metric_id", nullable = false)
    private Long metricId;

    @UpdateTimestamp
    @Column(name = "accessed_time")
    private LocalDateTime accessedTime;
}
