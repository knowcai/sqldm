package org.example.controller;

import lombok.RequiredArgsConstructor;
import org.example.entity.MetricDefinition;
import org.example.service.MetricApprovalService;
import org.example.service.MetricEngagementService;
import org.example.service.MetricService;
import org.example.service.MetricVersionAuditService;
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
    private final MetricVersionAuditService versionAuditService;
    private final MetricEngagementService engagementService;
    private final MetricApprovalService metricApprovalService;

    @PostMapping
    public ResponseEntity<Map<String, Object>> createMetric(@RequestBody MetricDefinition metric) {
        Map<String, Object> response = new HashMap<>();
        try {
            MetricDefinition created = metricService.createMetric(metric);
            response.put("success", true);
            response.put("data", created);
            response.put("warnings", created.getDuplicateWarnings());
            String msg = "PENDING_APPROVAL".equals(created.getStatus())
                    ? "已提交审批，等待主题管理员审核" : "指标创建成功";
            response.put("message", msg);
            response.put("pendingApproval", "PENDING_APPROVAL".equals(created.getStatus()));
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
            response.put("warnings", updated.getDuplicateWarnings());
            boolean pending = metricApprovalService.hasPendingUpdate(id);
            response.put("message", pending ? "变更已提交审批" : "指标更新成功");
            response.put("pendingApproval", pending);
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
    public ResponseEntity<Map<String, Object>> getAllMetrics(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "topicId", required = false) Long topicId,
            @RequestParam(value = "tag", required = false) String tag,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "sort", required = false) String sort) {
        Map<String, Object> response = new HashMap<>();
        List<MetricDefinition> metrics = metricService.listMetrics(keyword, topicId, status, tag, sort);
        response.put("success", true);
        response.put("data", metrics);
        response.put("total", metrics.size());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/mine")
    public ResponseEntity<Map<String, Object>> getMyMetrics() {
        Map<String, Object> response = new HashMap<>();
        List<MetricDefinition> metrics = engagementService.listMine();
        response.put("success", true);
        response.put("data", metrics);
        response.put("total", metrics.size());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/my-topics")
    public ResponseEntity<Map<String, Object>> getMyTopicMetrics() {
        Map<String, Object> response = new HashMap<>();
        List<MetricDefinition> metrics = engagementService.listMyTopicMetrics();
        response.put("success", true);
        response.put("data", metrics);
        response.put("total", metrics.size());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/favorites")
    public ResponseEntity<Map<String, Object>> getFavorites() {
        Map<String, Object> response = new HashMap<>();
        List<MetricDefinition> metrics = engagementService.listFavorites();
        response.put("success", true);
        response.put("data", metrics);
        response.put("total", metrics.size());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/recent")
    public ResponseEntity<Map<String, Object>> getRecent() {
        Map<String, Object> response = new HashMap<>();
        List<MetricDefinition> metrics = engagementService.listRecent();
        response.put("success", true);
        response.put("data", metrics);
        response.put("total", metrics.size());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/check-duplicates")
    public ResponseEntity<Map<String, Object>> checkDuplicates(@RequestBody MetricDefinition metric,
                                                                @RequestParam(value = "id", required = false) Long id) {
        Map<String, Object> response = new HashMap<>();
        List<String> warnings = metricService.checkDuplicateWarnings(metric, id);
        response.put("success", true);
        response.put("warnings", warnings);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/favorite")
    public ResponseEntity<Map<String, Object>> addFavorite(@PathVariable("id") Long id) {
        Map<String, Object> response = new HashMap<>();
        try {
            engagementService.addFavorite(id);
            response.put("success", true);
            response.put("message", "已收藏");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @DeleteMapping("/{id}/favorite")
    public ResponseEntity<Map<String, Object>> removeFavorite(@PathVariable("id") Long id) {
        Map<String, Object> response = new HashMap<>();
        try {
            engagementService.removeFavorite(id);
            response.put("success", true);
            response.put("message", "已取消收藏");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/search")
    public ResponseEntity<Map<String, Object>> searchMetrics(
            @RequestParam String keyword,
            @RequestParam(value = "topicId", required = false) Long topicId,
            @RequestParam(value = "tag", required = false) String tag,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "sort", required = false) String sort) {
        Map<String, Object> response = new HashMap<>();
        List<MetricDefinition> metrics = metricService.queryMetrics(keyword, topicId, status, tag, sort);
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

    @GetMapping("/{id}/versions")
    public ResponseEntity<Map<String, Object>> listVersions(@PathVariable("id") Long id) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", versionAuditService.listVersions(id));
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/audit-logs")
    public ResponseEntity<Map<String, Object>> listAuditLogs(@PathVariable("id") Long id) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", versionAuditService.listAuditLogs(id));
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
}
