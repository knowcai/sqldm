package org.sqldm.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

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
    private List<String> changedFields;
}
