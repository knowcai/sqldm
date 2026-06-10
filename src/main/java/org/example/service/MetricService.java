package org.example.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.example.entity.MetricDefinition;
import org.example.entity.Topic;
import org.example.repository.MetricRepository;
import org.example.repository.TopicRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Objects;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class MetricService {

    private static final Set<String> VALID_STAT_PERIODS = Set.of("DAY", "MONTH", "WEEK", "REALTIME");
    private static final Set<String> VALID_STATUSES = Set.of("DRAFT", "ACTIVE", "DISABLED");

    private final MetricRepository metricRepository;
    private final TopicRepository topicRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Transactional
    public MetricDefinition createMetric(MetricDefinition metric) {
        validateMetric(metric, null);

        if (metricRepository.findByMetricNameAndIsDeletedFalse(metric.getMetricName()).isPresent()) {
            throw new RuntimeException("指标名称已存在: " + metric.getMetricName());
        }
        if (metricRepository.findByMetricCodeAndIsDeletedFalse(metric.getMetricCode()).isPresent()) {
            throw new RuntimeException("指标编码已存在: " + metric.getMetricCode());
        }

        fillTopicInfo(metric);
        if (!StringUtils.hasText(metric.getStatus())) {
            metric.setStatus("ACTIVE");
        }

        String currentUser = getCurrentUsername();
        metric.setCreatedBy(currentUser);
        metric.setUpdatedBy(currentUser);
        metric.setIsDeleted(false);

        return metricRepository.save(metric);
    }

    @Transactional
    public MetricDefinition updateMetric(Long id, MetricDefinition metric) {
        MetricDefinition existing = metricRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("指标不存在: " + id));

        validateMetric(metric, id);

        if (!Objects.equals(existing.getMetricName(), metric.getMetricName())
                && metricRepository.findByMetricNameAndIsDeletedFalse(metric.getMetricName()).isPresent()) {
            throw new RuntimeException("指标名称已存在: " + metric.getMetricName());
        }
        if (!Objects.equals(existing.getMetricCode(), metric.getMetricCode())
                && metricRepository.findByMetricCodeAndIsDeletedFalse(metric.getMetricCode()).isPresent()) {
            throw new RuntimeException("指标编码已存在: " + metric.getMetricCode());
        }

        existing.setMetricName(metric.getMetricName());
        existing.setMetricCode(metric.getMetricCode());
        existing.setBusinessCaliber(metric.getBusinessCaliber());
        existing.setStatPeriod(metric.getStatPeriod());
        existing.setTopicId(metric.getTopicId());
        existing.setOwner(metric.getOwner());
        existing.setStatus(metric.getStatus());
        existing.setDataSource(metric.getDataSource());
        existing.setSqlTemplate(metric.getSqlTemplate());
        existing.setParamDefinition(metric.getParamDefinition());
        fillTopicInfo(existing);
        existing.setUpdatedBy(getCurrentUsername());

        return metricRepository.save(existing);
    }

    @Transactional
    public void deleteMetric(Long id) {
        MetricDefinition metric = metricRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("指标不存在: " + id));

        metric.setIsDeleted(true);
        metric.setUpdatedBy(getCurrentUsername());
        metricRepository.save(metric);
    }

    public List<MetricDefinition> getAllMetrics() {
        return metricRepository.findByIsDeletedFalseOrderByCreatedTimeDesc();
    }

    public MetricDefinition getMetricById(Long id) {
        return metricRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("指标不存在: " + id));
    }

    public List<MetricDefinition> searchMetrics(String keyword) {
        return metricRepository.searchMetrics(keyword);
    }

    public List<MetricDefinition> getMetricsByTopic(Long topicId) {
        return metricRepository.findByTopicIdAndIsDeletedFalseOrderByCreatedTimeDesc(topicId);
    }

    private void validateMetric(MetricDefinition metric, Long excludeId) {
        if (!StringUtils.hasText(metric.getMetricName())) {
            throw new RuntimeException("指标名称不能为空");
        }
        if (!StringUtils.hasText(metric.getMetricCode())) {
            throw new RuntimeException("指标编码不能为空");
        }
        if (!StringUtils.hasText(metric.getBusinessCaliber())) {
            throw new RuntimeException("业务口径不能为空");
        }
        if (metric.getTopicId() == null) {
            throw new RuntimeException("所属主题域不能为空");
        }
        if (!StringUtils.hasText(metric.getOwner())) {
            throw new RuntimeException("负责人不能为空");
        }
        if (!"DRAFT".equals(metric.getStatus())) {
            if (!StringUtils.hasText(metric.getDataSource())) {
                throw new RuntimeException("数据源不能为空");
            }
            if (!StringUtils.hasText(metric.getSqlTemplate())) {
                throw new RuntimeException("SQL模版不能为空");
            }
        }
        if (StringUtils.hasText(metric.getParamDefinition())) {
            try {
                objectMapper.readTree(metric.getParamDefinition());
            } catch (Exception e) {
                throw new RuntimeException("参数定义必须是合法的 JSON 格式");
            }
        }
        if (!StringUtils.hasText(metric.getStatus())) {
            throw new RuntimeException("状态不能为空");
        }
        if (!VALID_STATUSES.contains(metric.getStatus())) {
            throw new RuntimeException("无效的状态: " + metric.getStatus());
        }
        if (StringUtils.hasText(metric.getStatPeriod()) && !VALID_STAT_PERIODS.contains(metric.getStatPeriod())) {
            throw new RuntimeException("无效的统计周期: " + metric.getStatPeriod());
        }
    }

    private void fillTopicInfo(MetricDefinition metric) {
        Topic topic = topicRepository.findById(metric.getTopicId())
                .orElseThrow(() -> new RuntimeException("主题不存在: " + metric.getTopicId()));
        metric.setTopicName(topic.getTopicName());
    }

    private String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            return authentication.getName();
        }
        return "system";
    }
}
