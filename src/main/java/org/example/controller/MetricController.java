package org.example.controller;

import lombok.RequiredArgsConstructor;
import org.example.entity.MetricDefinition;
import org.example.service.MetricService;
import org.example.util.SqlParserUtil;
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

    /**
     * 创建指标
     */
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

    /**
     * 更新指标
     */
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

    /**
     * 删除指标
     */
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

    /**
     * 获取所有指标
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllMetrics() {
        Map<String, Object> response = new HashMap<>();
        List<MetricDefinition> metrics = metricService.getAllMetrics();
        response.put("success", true);
        response.put("data", metrics);
        response.put("total", metrics.size());
        return ResponseEntity.ok(response);
    }

    /**
     * 根据ID获取指标
     */
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

    /**
     * 根据类型获取指标
     */
    @GetMapping("/type/{type}")
    public ResponseEntity<Map<String, Object>> getMetricsByType(@PathVariable("type") String type) {
        Map<String, Object> response = new HashMap<>();
        List<MetricDefinition> metrics = metricService.getMetricsByType(type);
        response.put("success", true);
        response.put("data", metrics);
        response.put("total", metrics.size());
        return ResponseEntity.ok(response);
    }

    /**
     * 搜索指标
     */
    @GetMapping("/search")
    public ResponseEntity<Map<String, Object>> searchMetrics(@RequestParam String keyword) {
        Map<String, Object> response = new HashMap<>();
        List<MetricDefinition> metrics = metricService.searchMetrics(keyword);
        response.put("success", true);
        response.put("data", metrics);
        response.put("total", metrics.size());
        return ResponseEntity.ok(response);
    }
    
    /**
     * 根据主题获取指标
     */
    @GetMapping("/topic/{topicId}")
    public ResponseEntity<Map<String, Object>> getMetricsByTopic(@PathVariable("topicId") Long topicId) {
        Map<String, Object> response = new HashMap<>();
        List<MetricDefinition> metrics = metricService.getMetricsByTopic(topicId);
        response.put("success", true);
        response.put("data", metrics);
        response.put("total", metrics.size());
        return ResponseEntity.ok(response);
    }

    /**
     * 解析SQL语句
     */
    @PostMapping("/parse-sql")
    public ResponseEntity<Map<String, Object>> parseSql(@RequestBody Map<String, String> request) {
        Map<String, Object> response = new HashMap<>();
        try {
            String sql = request.get("sql");
            if (sql == null || sql.trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "SQL语句不能为空");
                return ResponseEntity.badRequest().body(response);
            }
            
            Map<String, Object> parsedResult = SqlParserUtil.parseSql(sql);
            response.put("success", true);
            response.put("data", parsedResult);
            response.put("message", "SQL解析成功");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "SQL解析失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 根据指标ID生成SQL
     */
    @GetMapping("/{id}/generate-sql")
    public ResponseEntity<Map<String, Object>> generateSql(@PathVariable("id") Long id) {
        Map<String, Object> response = new HashMap<>();
        try {
            String sql = metricService.generateSqlFromMetric(id);
            response.put("success", true);
            response.put("data", sql);
            response.put("message", "SQL生成成功");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "SQL生成失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
}
