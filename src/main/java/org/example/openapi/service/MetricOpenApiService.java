package org.example.openapi.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.example.entity.MetricDefinition;
import org.example.entity.MetricVersion;
import org.example.openapi.dto.MetricOpenDto;
import org.example.openapi.dto.MetricParamDto;
import org.example.openapi.util.OpenApiIpHelper;
import org.example.repository.MetricRepository;
import org.example.repository.MetricVersionRepository;
import org.example.service.ApiClientService;
import org.example.service.MetricTagService;
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
    private final ApiClientService apiClientService;
    private final MetricTagService metricTagService;
    private final ObjectMapper objectMapper;

    public void validateAccess(String apiKey, String clientIp) {
        if (apiClientService.hasActiveClients()) {
            apiClientService.validateApiKey(apiKey, clientIp);
        }
    }

    public List<MetricOpenDto> listActiveMetrics(String clientIp) {
        return metricRepository.findByStatusAndIsDeletedFalseOrderByCreatedTimeDesc(ACTIVE_STATUS)
                .stream()
                .filter(metric -> OpenApiIpHelper.isIpAllowed(metric.getAllowedIps(), clientIp))
                .map(metric -> toOpenDto(metric, null))
                .toList();
    }

    public MetricOpenDto getActiveMetricByCode(String metricCode, Integer version, String clientIp) {
        MetricDefinition metric = metricRepository
                .findByMetricCodeAndStatusAndIsDeletedFalse(metricCode, ACTIVE_STATUS)
                .orElseThrow(() -> new RuntimeException("指标不存在或未启用: " + metricCode));
        assertIpAllowed(metric, clientIp);
        if (version != null) {
            return getVersionDto(metric, version);
        }
        return toOpenDto(metric, null);
    }

    public MetricOpenDto getActiveMetricById(Long id, Integer version, String clientIp) {
        MetricDefinition metric = metricRepository
                .findByIdAndStatusAndIsDeletedFalse(id, ACTIVE_STATUS)
                .orElseThrow(() -> new RuntimeException("指标不存在或未启用: " + id));
        assertIpAllowed(metric, clientIp);
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

    private void assertIpAllowed(MetricDefinition metric, String clientIp) {
        if (!OpenApiIpHelper.isIpAllowed(metric.getAllowedIps(), clientIp)) {
            throw new RuntimeException("IP 不在允许访问列表: " + OpenApiIpHelper.normalizeIp(clientIp));
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
        if (metric.getId() != null) {
            dto.setTags(metricTagService.getTagNames(metric.getId()));
        }
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
