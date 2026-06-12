package org.example.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "metric_favorite", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "metric_id"}))
public class MetricFavorite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "metric_id", nullable = false)
    private Long metricId;

    @CreationTimestamp
    @Column(name = "created_time", updatable = false)
    private LocalDateTime createdTime;
}
