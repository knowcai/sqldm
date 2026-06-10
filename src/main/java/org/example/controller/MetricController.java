package org.example.controller;

import lombok.RequiredArgsConstructor;
import org.example.entity.MetricDefinition;
import org.example.service.MetricService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/metrics")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class MetricController {

    private final MetricService metricService;

    @PostMapping
    public ResponseEntity<Map<String, Object>> createMetric(@RequestBody MetricDefinition metric) {
        Map<String, Object> response = new HashMap<>();
        try {
            MetricDefinition created = metricService.createMetric(metric);
            response.put("success", true);
            response.put("data", created);
            response.put("message", "指标创建成功");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateMetric(@PathVariable("id") Long id,
                                                            @RequestBody MetricDefinition metric) {
        Map<String, Object> response = new HashMap<>();
        try {
            MetricDefinition updated = metricService.updateMetric(id, metric);
            response.put("success", true);
            response.put("data", updated);
            response.put("message", "指标更新成功");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteMetric(@PathVariable("id") Long id) {
        Map<String, Object> response = new HashMap<>();
        try {
            metricService.deleteMetric(id);
            response.put("success", true);
            response.put("message", "指标删除成功");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllMetrics() {
        Map<String, Object> response = new HashMap<>();
        List<MetricDefinition> metrics = metricService.getAllMetrics();
        response.put("success", true);
        response.put("data", metrics);
        response.put("total", metrics.size());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getMetricById(@PathVariable("id") Long id) {
        Map<String, Object> response = new HashMap<>();
        try {
            MetricDefinition metric = metricService.getMetricById(id);
            response.put("success", true);
            response.put("data", metric);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/search")
    public ResponseEntity<Map<String, Object>> searchMetrics(@RequestParam String keyword) {
        Map<String, Object> response = new HashMap<>();
        List<MetricDefinition> metrics = metricService.searchMetrics(keyword);
        response.put("success", true);
        response.put("data", metrics);
        response.put("total", metrics.size());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/topic/{topicId}")
    public ResponseEntity<Map<String, Object>> getMetricsByTopic(@PathVariable("topicId") Long topicId) {
        Map<String, Object> response = new HashMap<>();
        List<MetricDefinition> metrics = metricService.getMetricsByTopic(topicId);
        response.put("success", true);
        response.put("data", metrics);
        response.put("total", metrics.size());
        return ResponseEntity.ok(response);
    }
}
