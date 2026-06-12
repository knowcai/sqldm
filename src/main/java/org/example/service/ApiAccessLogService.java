package org.example.service;

import lombok.RequiredArgsConstructor;
import org.example.entity.ApiAccessLog;
import org.example.repository.ApiAccessLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ApiAccessLogService {

    private final ApiAccessLogRepository repository;

    @Transactional
    public void log(String apiKey, String clientIp, String metricCode, Long metricId,
                    Integer versionNo, boolean success, String errorMessage, long elapsedMs) {
        ApiAccessLog log = new ApiAccessLog();
        log.setApiKeyHint(maskApiKey(apiKey));
        log.setClientIp(clientIp);
        log.setMetricCode(metricCode);
        log.setMetricId(metricId);
        log.setVersionNo(versionNo);
        log.setSuccess(success);
        log.setErrorMessage(truncate(errorMessage, 500));
        log.setElapsedMs(elapsedMs);
        repository.save(log);
    }

    public List<ApiAccessLog> listRecent() {
        return repository.findTop200ByOrderByCreatedTimeDesc();
    }

    public Map<String, Object> stats() {
        long total = repository.count();
        long successCount = repository.countBySuccessTrue();
        long failCount = total - successCount;
        double failRate = total == 0 ? 0 : (failCount * 100.0 / total);

        List<Map<String, Object>> byMetric = repository.topMetricsByCallCount().stream()
                .limit(10)
                .map(row -> Map.<String, Object>of(
                        "metricCode", row[0] != null ? row[0] : "-",
                        "count", row[1]))
                .toList();
        List<Map<String, Object>> byKey = repository.topApiKeysByCallCount().stream()
                .map(row -> Map.<String, Object>of(
                        "apiKeyHint", row[0] != null ? row[0] : "-",
                        "count", row[1]))
                .toList();

        return Map.of(
                "total", total,
                "successCount", successCount,
                "failCount", failCount,
                "failRatePercent", Math.round(failRate * 100) / 100.0,
                "topMetrics", byMetric,
                "topApiKeys", byKey
        );
    }

    private String maskApiKey(String apiKey) {
        if (!StringUtils.hasText(apiKey)) {
            return null;
        }
        String key = apiKey.trim();
        if (key.length() <= 8) {
            return "****";
        }
        return key.substring(0, 4) + "****" + key.substring(key.length() - 4);
    }

    private String truncate(String s, int max) {
        if (s == null) {
            return null;
        }
        return s.length() <= max ? s : s.substring(0, max);
    }
}
