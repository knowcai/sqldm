package org.example.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "webhook_config")
public class WebhookConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "url", nullable = false, length = 500)
    private String url;

    @Column(name = "secret", length = 200)
    private String secret;

    /** JSON 数组，如 ["METRIC_CREATED","METRIC_UPDATED"] */
    @Column(name = "events", nullable = false, columnDefinition = "TEXT")
    private String events;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_time", updatable = false)
    private LocalDateTime createdTime;

    @UpdateTimestamp
    @Column(name = "updated_time")
    private LocalDateTime updatedTime;
}
