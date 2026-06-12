package org.example.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.config.SqldmProperties;
import org.example.entity.MetricDefinition;
import org.example.entity.WebhookConfig;
import org.example.repository.WebhookConfigRepository;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebhookNotificationService {

    private final WebhookConfigRepository webhookRepository;
    private final SqldmProperties properties;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate = new RestTemplate();

    @Async
    public void notify(String event, MetricDefinition metric, String detail) {
        List<WebhookConfig> hooks = webhookRepository.findByIsActiveTrue();
        for (WebhookConfig hook : hooks) {
            if (!supportsEvent(hook, event)) {
                continue;
            }
            try {
                Map<String, Object> body = new HashMap<>();
                body.put("event", event);
                body.put("timestamp", LocalDateTime.now().toString());
                body.put("detail", detail);
                body.put("metric", Map.of(
                        "id", metric.getId(),
                        "metricCode", metric.getMetricCode(),
                        "metricName", metric.getMetricName(),
                        "status", metric.getStatus(),
                        "owner", metric.getOwner()
                ));
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                if (StringUtils.hasText(hook.getSecret())) {
                    headers.set("X-Webhook-Secret", hook.getSecret());
                }
                restTemplate.postForEntity(hook.getUrl(), new HttpEntity<>(body, headers), String.class);
            } catch (Exception e) {
                log.warn("Webhook 发送失败 [{}] {}: {}", hook.getName(), hook.getUrl(), e.getMessage());
            }
        }
    }

    private boolean supportsEvent(WebhookConfig hook, String event) {
        try {
            List<String> events = objectMapper.readValue(hook.getEvents(), new TypeReference<List<String>>() {});
            return events.contains(event) || events.contains("*");
        } catch (Exception e) {
            return false;
        }
    }
}
