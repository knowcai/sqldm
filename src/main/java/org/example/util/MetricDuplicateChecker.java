package org.example.util;

import org.example.entity.MetricDefinition;
import org.example.repository.MetricRepository;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Component
public class MetricDuplicateChecker {

    private final MetricRepository metricRepository;

    public MetricDuplicateChecker(MetricRepository metricRepository) {
        this.metricRepository = metricRepository;
    }

    public List<String> findWarnings(MetricDefinition metric, Long excludeId) {
        List<String> warnings = new ArrayList<>();
        if (metric.getTopicId() == null) {
            return warnings;
        }
        List<MetricDefinition> peers = metricRepository
                .findByTopicIdAndIsDeletedFalseOrderByCreatedTimeDesc(metric.getTopicId());
        String name = metric.getMetricName() != null ? metric.getMetricName().trim().toLowerCase() : "";
        String code = metric.getMetricCode() != null ? metric.getMetricCode().trim().toLowerCase() : "";

        for (MetricDefinition peer : peers) {
            if (excludeId != null && Objects.equals(peer.getId(), excludeId)) {
                continue;
            }
            if (StringUtils.hasText(name) && name.equalsIgnoreCase(peer.getMetricName())) {
                warnings.add("同主题下已存在同名指标: " + peer.getMetricName());
            }
            if (StringUtils.hasText(code) && code.equalsIgnoreCase(peer.getMetricCode())) {
                warnings.add("同主题下已存在同编码指标: " + peer.getMetricCode());
            }
            if (StringUtils.hasText(name) && StringUtils.hasText(peer.getMetricName())) {
                String peerName = peer.getMetricName().toLowerCase();
                if (!name.equals(peerName) && (name.contains(peerName) || peerName.contains(name))) {
                    warnings.add("同主题下存在名称相似的指标: " + peer.getMetricName());
                }
            }
        }
        return warnings.stream().distinct().toList();
    }
}
