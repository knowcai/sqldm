package org.example.openapi.controller;

import lombok.RequiredArgsConstructor;
import org.example.openapi.dto.MetricOpenDto;
import org.example.openapi.service.MetricOpenApiService;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 对外 Open API：供其他系统查询已启用指标的 SQL 模版与参数定义。
 */
@RestController
@RequestMapping("/api/open/metrics")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class MetricOpenApiController {

    private final MetricOpenApiService metricOpenApiService;

    /**
     * 查询已启用指标。
     * - 无参数：返回全部已启用指标列表
     * - code：按指标编码查询单个
     * - id：按指标 ID 查询单个
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> queryMetrics(
            @RequestParam(value = "code", required = false) String code,
            @RequestParam(value = "id", required = false) Long id) {
        Map<String, Object> response = new HashMap<>();
        try {
            if (StringUtils.hasText(code)) {
                MetricOpenDto metric = metricOpenApiService.getActiveMetricByCode(code.trim());
                response.put("success", true);
                response.put("data", metric);
                return ResponseEntity.ok(response);
            }
            if (id != null) {
                MetricOpenDto metric = metricOpenApiService.getActiveMetricById(id);
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
