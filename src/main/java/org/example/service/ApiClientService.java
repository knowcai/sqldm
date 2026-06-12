package org.example.service;

import lombok.RequiredArgsConstructor;
import org.example.entity.ApiClient;
import org.example.openapi.util.OpenApiIpHelper;
import org.example.repository.ApiClientRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ApiClientService {

    private final ApiClientRepository repository;

    public List<ApiClient> listAll() {
        return repository.findAll();
    }

    public ApiClient getById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("API 客户端不存在: " + id));
    }

    @Transactional
    public ApiClient create(ApiClient client) {
        if (!StringUtils.hasText(client.getClientName())) {
            throw new RuntimeException("客户端名称不能为空");
        }
        if (!StringUtils.hasText(client.getApiKey())) {
            client.setApiKey(generateApiKey());
        }
        if (repository.existsByApiKey(client.getApiKey())) {
            throw new RuntimeException("API Key 已存在");
        }
        OpenApiIpHelper.validateAllowedIpsJson(client.getAllowedIps());
        if (client.getIsActive() == null) {
            client.setIsActive(true);
        }
        return repository.save(client);
    }

    @Transactional
    public ApiClient update(Long id, ApiClient client) {
        ApiClient existing = getById(id);
        existing.setClientName(client.getClientName());
        existing.setDescription(client.getDescription());
        existing.setAllowedIps(client.getAllowedIps());
        existing.setIsActive(client.getIsActive() != null ? client.getIsActive() : true);
        OpenApiIpHelper.validateAllowedIpsJson(existing.getAllowedIps());
        return repository.save(existing);
    }

    @Transactional
    public ApiClient regenerateKey(Long id) {
        ApiClient existing = getById(id);
        existing.setApiKey(generateApiKey());
        return repository.save(existing);
    }

    public ApiClient validateApiKey(String apiKey, String clientIp) {
        if (!StringUtils.hasText(apiKey)) {
            throw new RuntimeException("缺少 API Key，请在请求头 X-API-Key 中提供");
        }
        ApiClient client = repository.findByApiKeyAndIsActiveTrue(apiKey.trim())
                .orElseThrow(() -> new RuntimeException("无效的 API Key"));
        if (!OpenApiIpHelper.isIpAllowed(client.getAllowedIps(), clientIp)) {
            throw new RuntimeException("IP 不在 API 客户端允许列表: " + OpenApiIpHelper.normalizeIp(clientIp));
        }
        return client;
    }

    public boolean hasActiveClients() {
        return !repository.findByIsActiveTrue().isEmpty();
    }

    private String generateApiKey() {
        byte[] bytes = new byte[24];
        new SecureRandom().nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }
}
