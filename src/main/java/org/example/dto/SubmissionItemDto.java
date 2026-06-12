package org.example.dto;

import lombok.Data;
import org.example.entity.MetricDefinition;

import java.time.LocalDateTime;

@Data
public class SubmissionItemDto {
    private String requestType;
    private Long requestId;
    private Long metricId;
    private String metricName;
    private String metricCode;
    private String topicName;
    private String status;
    private String reviewComment;
    private LocalDateTime submittedTime;
    private LocalDateTime reviewedTime;
    private MetricDefinition metric;
}
