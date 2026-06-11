package org.example.openapi.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.example.entity.MetricDefinition;
import org.example.openapi.dto.MetricOpenDto;
import org.example.openapi.dto.MetricParamDto;
import org.example.repository.MetricRepository;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MetricOpenApiService {

    private static final String ACTIVE_STATUS = "ACTIVE";

    private final MetricRepository metricRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public List<MetricOpenDto> listActiveMetrics() {
        return metricRepository.findByStatusAndIsDeletedFalseOrderByCreatedTimeDesc(ACTIVE_STATUS)
                .stream()
                .map(this::toOpenDto)
                .toList();
    }

    public MetricOpenDto getActiveMetricByCode(String metricCode) {
        MetricDefinition metric = metricRepository
                .findByMetricCodeAndStatusAndIsDeletedFalse(metricCode, ACTIVE_STATUS)
                .orElseThrow(() -> new RuntimeException("指标不存在或未启用: " + metricCode));
        return toOpenDto(metric);
    }

    public MetricOpenDto getActiveMetricById(Long id) {
        MetricDefinition metric = metricRepository
                .findByIdAndStatusAndIsDeletedFalse(id, ACTIVE_STATUS)
                .orElseThrow(() -> new RuntimeException("指标不存在或未启用: " + id));
        return toOpenDto(metric);
    }

    private MetricOpenDto toOpenDto(MetricDefinition metric) {
        MetricOpenDto dto = new MetricOpenDto();
        dto.setMetricCode(metric.getMetricCode());
        dto.setMetricName(metric.getMetricName());
        dto.setDataSource(metric.getDataSource());
        dto.setSqlTemplate(metric.getSqlTemplate());
        dto.setParams(parseParams(metric.getParamDefinition()));
        return dto;
    }

    private List<MetricParamDto> parseParams(String paramDefinition) {
        if (!StringUtils.hasText(paramDefinition)) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(paramDefinition, new TypeReference<List<MetricParamDto>>() {});
        } catch (Exception e) {
            throw new RuntimeException("参数定义格式错误");
        }
    }
}
