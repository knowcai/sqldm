package org.sqldm.openapi.dto;

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
    private Integer version;
    private LocalDateTime updatedTime;
}
