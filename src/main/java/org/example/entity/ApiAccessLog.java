package org.example.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "api_access_log")
public class ApiAccessLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "api_key_hint", length = 16)
    private String apiKeyHint;

    @Column(name = "client_ip", length = 64)
    private String clientIp;

    @Column(name = "metric_code", length = 100)
    private String metricCode;

    @Column(name = "metric_id")
    private Long metricId;

    @Column(name = "version_no")
    private Integer versionNo;

    @Column(name = "success", nullable = false)
    private Boolean success;

    @Column(name = "error_message", length = 500)
    private String errorMessage;

    @Column(name = "elapsed_ms")
    private Long elapsedMs;

    @CreationTimestamp
    @Column(name = "created_time", updatable = false)
    private LocalDateTime createdTime;
}
