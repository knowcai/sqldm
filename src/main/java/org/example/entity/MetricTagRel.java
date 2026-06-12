package org.example.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "metric_tag_rel", uniqueConstraints = @UniqueConstraint(columnNames = {"metric_id", "tag_id"}))
public class MetricTagRel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "metric_id", nullable = false)
    private Long metricId;

    @Column(name = "tag_id", nullable = false)
    private Long tagId;
}
