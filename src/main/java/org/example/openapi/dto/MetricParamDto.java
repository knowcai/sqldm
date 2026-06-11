package org.example.openapi.dto;

import lombok.Data;

@Data
public class MetricParamDto {
    private String name;
    private String type;
    private Boolean required;
    private String description;
}
