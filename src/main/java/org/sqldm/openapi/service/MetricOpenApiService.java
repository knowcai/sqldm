package org.sqldm.openapi.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.sqldm.entity.MetricDefinition;
import org.sqldm.entity.MetricVersion;
import org.sqldm.openapi.dto.MetricOpenDto;
import org.sqldm.openapi.dto.MetricParamDto;
import org.sqldm.repository.MetricRepository;
import org.sqldm.repository.MetricVersionRepository;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MetricOpenApiService {

    private static final String ACTIVE_STATUS = "ACTIVE";

    private final MetricRepository metricRepository;
    private final MetricVersionRepository versionRepository;
    private final ObjectMapper objectMapper;

    public List<MetricOpenDto> listActiveMetrics() {
        return metricRepository.findByStatusAndIsDeletedFalseOrderByCreatedTimeDesc(ACTIVE_STATUS)
                .stream()
                .map(metric -> toOpenDto(metric, null))
                .toList();
    }

    public MetricOpenDto getActiveMetricByCode(String metricCode, Integer version) {
        MetricDefinition metric = metricRepository
                .findByMetricCodeAndStatusAndIsDeletedFalse(metricCode, ACTIVE_STATUS)
                .orElseThrow(() -> new RuntimeException("指标不存在或未启用: " + metricCode));
        if (version != null) {
            return getVersionDto(metric, version);
        }
        return toOpenDto(metric, null);
    }

    public MetricOpenDto getActiveMetricById(Long id, Integer version) {
        MetricDefinition metric = metricRepository
                .findByIdAndStatusAndIsDeletedFalse(id, ACTIVE_STATUS)
                .orElseThrow(() -> new RuntimeException("指标不存在或未启用: " + id));
        if (version != null) {
            return getVersionDto(metric, version);
        }
        return toOpenDto(metric, null);
    }

    private MetricOpenDto getVersionDto(MetricDefinition metric, Integer version) {
        MetricVersion mv = versionRepository.findByMetricIdAndVersionNo(metric.getId(), version)
                .orElseThrow(() -> new RuntimeException("版本不存在: v" + version));
        try {
            MetricDefinition snapshot = objectMapper.readValue(mv.getSnapshot(), MetricDefinition.class);
            MetricOpenDto dto = toOpenDto(snapshot, version);
            dto.setVersion(version);
            return dto;
        } catch (Exception e) {
            throw new RuntimeException("版本快照解析失败");
        }
    }

    private MetricOpenDto toOpenDto(MetricDefinition metric, Integer version) {
        MetricOpenDto dto = new MetricOpenDto();
        dto.setMetricCode(metric.getMetricCode());
        dto.setMetricName(metric.getMetricName());
        dto.setBusinessCaliber(metric.getBusinessCaliber());
        dto.setStatPeriod(metric.getStatPeriod());
        dto.setTopicName(metric.getTopicName());
        dto.setDataSource(metric.getDataSource());
        dto.setSqlTemplate(metric.getSqlTemplate());
        dto.setParams(parseParams(metric.getParamDefinition()));
        dto.setUpdatedTime(metric.getUpdatedTime());
        if (version != null) {
            dto.setVersion(version);
        } else if (metric.getId() != null) {
            versionRepository.findTopByMetricIdOrderByVersionNoDesc(metric.getId())
                    .ifPresent(v -> dto.setVersion(v.getVersionNo()));
        }
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
