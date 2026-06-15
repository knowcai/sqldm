package org.sqldm.openapi.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.sqldm.openapi.dto.MetricOpenDto;
import org.sqldm.openapi.service.MetricOpenApiService;
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

    @RequestMapping(method = {RequestMethod.GET, RequestMethod.POST})
    @Operation(summary = "查询已启用指标定义", description = "支持 code、id、version 参数；GET/POST 均可；列表时返回全部 ACTIVE 指标")
    public ResponseEntity<Map<String, Object>> queryMetrics(
            @RequestParam(value = "code", required = false) String code,
            @RequestParam(value = "id", required = false) Long id,
            @RequestParam(value = "version", required = false) Integer version,
            @RequestBody(required = false) Map<String, Object> body) {
        if (body != null) {
            if (!StringUtils.hasText(code) && body.get("code") != null) {
                code = String.valueOf(body.get("code"));
            }
            if (id == null && body.get("id") instanceof Number) {
                id = ((Number) body.get("id")).longValue();
            }
            if (version == null && body.get("version") instanceof Number) {
                version = ((Number) body.get("version")).intValue();
            }
        }
        Map<String, Object> response = new HashMap<>();
        try {
            if (StringUtils.hasText(code)) {
                MetricOpenDto metric = metricOpenApiService.getActiveMetricByCode(code.trim(), version);
                response.put("success", true);
                response.put("data", metric);
                return ResponseEntity.ok(response);
            }
            if (id != null) {
                MetricOpenDto metric = metricOpenApiService.getActiveMetricById(id, version);
                response.put("success", true);
                response.put("data", metric);
                return ResponseEntity.ok(response);
            }

            List<MetricOpenDto> metrics = metricOpenApiService.listActiveMetrics();
            response.put("success", true);
            response.put("data", metrics);
            response.put("total", metrics.size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
}
