package org.example.openapi.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.example.openapi.dto.MetricOpenDto;
import org.example.openapi.service.MetricOpenApiService;
import org.example.openapi.util.OpenApiIpHelper;
import org.example.service.ApiAccessLogService;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/open/metrics")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Tag(name = "Open API - 指标定义")
public class MetricOpenApiController {

    private final MetricOpenApiService metricOpenApiService;
    private final ApiAccessLogService apiAccessLogService;

    @GetMapping
    @Operation(summary = "查询已启用指标定义", description = "支持 code、id、version 参数；列表时返回全部 ACTIVE 指标")
    public ResponseEntity<Map<String, Object>> queryMetrics(
            @RequestParam(value = "code", required = false) String code,
            @RequestParam(value = "id", required = false) Long id,
            @RequestParam(value = "version", required = false) Integer version,
            @RequestHeader(value = "X-API-Key", required = false) String apiKey,
            HttpServletRequest request) {
        long start = System.currentTimeMillis();
        Map<String, Object> response = new HashMap<>();
        String clientIp = OpenApiIpHelper.resolveClientIp(request);
        String metricCode = StringUtils.hasText(code) ? code.trim() : null;
        try {
            metricOpenApiService.validateAccess(apiKey, clientIp);

            if (StringUtils.hasText(code)) {
                MetricOpenDto metric = metricOpenApiService.getActiveMetricByCode(code.trim(), version, clientIp);
                response.put("success", true);
                response.put("data", metric);
                logAccess(apiKey, clientIp, metric.getMetricCode(), id, version, true, null, start);
                return ResponseEntity.ok(response);
            }
            if (id != null) {
                MetricOpenDto metric = metricOpenApiService.getActiveMetricById(id, version, clientIp);
                response.put("success", true);
                response.put("data", metric);
                logAccess(apiKey, clientIp, metric.getMetricCode(), id, version, true, null, start);
                return ResponseEntity.ok(response);
            }

            List<MetricOpenDto> metrics = metricOpenApiService.listActiveMetrics(clientIp);
            response.put("success", true);
            response.put("data", metrics);
            response.put("total", metrics.size());
            logAccess(apiKey, clientIp, null, null, null, true, null, start);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            logAccess(apiKey, clientIp, metricCode, id, version, false, e.getMessage(), start);
            return ResponseEntity.badRequest().body(response);
        }
    }

    private void logAccess(String apiKey, String clientIp, String metricCode, Long metricId,
                           Integer version, boolean success, String error, long start) {
        apiAccessLogService.log(apiKey, clientIp, metricCode, metricId, version, success, error,
                System.currentTimeMillis() - start);
    }
}
