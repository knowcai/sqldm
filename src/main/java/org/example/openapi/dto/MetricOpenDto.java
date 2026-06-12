package org.example.openapi.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class MetricOpenDto {
    private String metricCode;
    private String metricName;
    private String businessCaliber;
    private String statPeriod;
    private String topicName;
    private String dataSource;
    private String sqlTemplate;
    private List<MetricParamDto> params;
    private List<String> tags;
    private Integer version;
    private LocalDateTime updatedTime;
}
