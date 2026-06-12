package org.example.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.example.entity.WebhookConfig;
import org.example.repository.WebhookConfigRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class WebhookConfigService {

    private final WebhookConfigRepository repository;
    private final ObjectMapper objectMapper;

    public List<WebhookConfig> listAll() {
        return repository.findAll();
    }

    @Transactional
    public WebhookConfig create(WebhookConfig config) {
        validate(config);
        if (config.getIsActive() == null) {
            config.setIsActive(true);
        }
        return repository.save(config);
    }

    @Transactional
    public WebhookConfig update(Long id, WebhookConfig config) {
        WebhookConfig existing = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Webhook 不存在"));
        validate(config);
        existing.setName(config.getName());
        existing.setUrl(config.getUrl());
        existing.setSecret(config.getSecret());
        existing.setEvents(config.getEvents());
        existing.setIsActive(config.getIsActive() != null ? config.getIsActive() : true);
        return repository.save(existing);
    }

    @Transactional
    public void delete(Long id) {
        WebhookConfig existing = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Webhook 不存在"));
        existing.setIsActive(false);
        repository.save(existing);
    }

    private void validate(WebhookConfig config) {
        if (!StringUtils.hasText(config.getName())) {
            throw new RuntimeException("名称不能为空");
        }
        if (!StringUtils.hasText(config.getUrl())) {
            throw new RuntimeException("URL 不能为空");
        }
        if (!StringUtils.hasText(config.getEvents())) {
            throw new RuntimeException("订阅事件不能为空");
        }
        try {
            objectMapper.readTree(config.getEvents());
        } catch (Exception e) {
            throw new RuntimeException("events 必须是 JSON 数组");
        }
    }
}
