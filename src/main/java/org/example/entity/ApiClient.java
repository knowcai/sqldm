package org.example.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "api_client")
public class ApiClient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "client_name", nullable = false, length = 100)
    private String clientName;

    @Column(name = "api_key", nullable = false, unique = true, length = 64)
    private String apiKey;

    /** JSON 数组，空表示不限制 IP */
    @Column(name = "allowed_ips", columnDefinition = "TEXT")
    private String allowedIps;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "description", length = 500)
    private String description;

    @CreationTimestamp
    @Column(name = "created_time", updatable = false)
    private LocalDateTime createdTime;

    @UpdateTimestamp
    @Column(name = "updated_time")
    private LocalDateTime updatedTime;
}
