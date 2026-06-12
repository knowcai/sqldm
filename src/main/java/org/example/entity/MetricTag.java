package org.example.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "metric_tag")
public class MetricTag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tag_name", nullable = false, unique = true, length = 50)
    private String tagName;

    @CreationTimestamp
    @Column(name = "created_time", updatable = false)
    private LocalDateTime createdTime;
}
