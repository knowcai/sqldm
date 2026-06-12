package org.example.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.example.entity.MetricAuditLog;
import org.example.entity.MetricDefinition;
import org.example.entity.MetricVersion;
import org.example.repository.MetricAuditLogRepository;
import org.example.repository.MetricVersionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MetricVersionAuditService {

    private final MetricVersionRepository versionRepository;
    private final MetricAuditLogRepository auditLogRepository;
    private final CurrentUserService currentUserService;
    private final ObjectMapper objectMapper;

    public List<MetricVersion> listVersions(Long metricId) {
        return versionRepository.findByMetricIdOrderByVersionNoDesc(metricId);
    }

    public List<MetricAuditLog> listAuditLogs(Long metricId) {
        return auditLogRepository.findByMetricIdOrderByCreatedTimeDesc(metricId);
    }

    @Transactional
    public void logAction(Long metricId, String action, String detail) {
        MetricAuditLog log = new MetricAuditLog();
        log.setMetricId(metricId);
        log.setAction(action);
        log.setOperator(getCurrentUsername());
        log.setDetail(detail);
        auditLogRepository.save(log);
    }

    @Transactional
    public void snapshotIfActive(MetricDefinition metric, String changeSummary) {
        if (!"ACTIVE".equals(metric.getStatus())) {
            return;
        }
        try {
            int nextVersion = versionRepository.findTopByMetricIdOrderByVersionNoDesc(metric.getId())
                    .map(v -> v.getVersionNo() + 1)
                    .orElse(1);

            MetricVersion version = new MetricVersion();
            version.setMetricId(metric.getId());
            version.setVersionNo(nextVersion);
            version.setSnapshot(objectMapper.writeValueAsString(metric));
            version.setChangeSummary(changeSummary);
            version.setCreatedBy(getCurrentUsername());
            versionRepository.save(version);
        } catch (Exception e) {
            throw new RuntimeException("保存指标版本失败: " + e.getMessage());
        }
    }

    private String getCurrentUsername() {
        return currentUserService.getCurrentUsername();
    }
}
