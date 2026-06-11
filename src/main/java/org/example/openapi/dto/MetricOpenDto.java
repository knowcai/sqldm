package org.example.openapi.dto;

import lombok.Data;

import java.util.List;

@Data
public class MetricOpenDto {
    private String metricCode;
    private String metricName;
    private String dataSource;
    private String sqlTemplate;
    private List<MetricParamDto> params;
}
