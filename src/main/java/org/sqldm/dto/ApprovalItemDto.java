package org.sqldm.dto;

import lombok.Data;
import org.sqldm.entity.MetricDefinition;

import java.time.LocalDateTime;

@Data
public class ApprovalItemDto {
    private String requestType;
    private Long requestId;
    private Long metricId;
    private Long topicId;
    private String topicName;
    private String metricName;
    private String metricCode;
    private String submittedBy;
    private LocalDateTime submittedTime;
    private MetricDefinition preview;
    private MetricDefinition current;
    private java.util.List<String> changedFields;
}
