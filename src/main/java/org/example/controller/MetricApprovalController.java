package org.example.controller;

import lombok.RequiredArgsConstructor;
import org.example.service.MetricApprovalService;
import org.example.service.MetricService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/approvals")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class MetricApprovalController {

    private final MetricApprovalService metricApprovalService;
    private final MetricService metricService;

    @GetMapping("/info")
    public ResponseEntity<Map<String, Object>> approverInfo() {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("canApprove", metricApprovalService.canCurrentUserApprove());
        response.put("pendingCount", metricApprovalService.countPendingForCurrentUser());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/pending")
    public ResponseEntity<Map<String, Object>> listPending() {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", metricApprovalService.listPendingForCurrentUser());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/my-submissions")
    public ResponseEntity<Map<String, Object>> listMySubmissions() {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", metricApprovalService.listMySubmissions());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/create/{metricId}/approve")
    public ResponseEntity<Map<String, Object>> approveCreate(
            @PathVariable("metricId") Long metricId,
            @RequestBody(required = false) Map<String, String> body) {
        Map<String, Object> response = new HashMap<>();
        try {
            String comment = body != null ? body.get("comment") : null;
            response.put("success", true);
            response.put("data", metricService.approveCreate(metricId, comment));
            response.put("message", "已通过新增申请");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/create/{metricId}/reject")
    public ResponseEntity<Map<String, Object>> rejectCreate(
            @PathVariable("metricId") Long metricId,
            @RequestBody(required = false) Map<String, String> body) {
        Map<String, Object> response = new HashMap<>();
        try {
            String comment = body != null ? body.get("comment") : null;
            metricService.rejectCreate(metricId, comment);
            response.put("success", true);
            response.put("message", "已驳回新增申请");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/update/{requestId}/approve")
    public ResponseEntity<Map<String, Object>> approveUpdate(
            @PathVariable("requestId") Long requestId,
            @RequestBody(required = false) Map<String, String> body) {
        Map<String, Object> response = new HashMap<>();
        try {
            String comment = body != null ? body.get("comment") : null;
            response.put("success", true);
            response.put("data", metricService.approveUpdate(requestId, comment));
            response.put("message", "已通过变更申请");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/update/{requestId}/reject")
    public ResponseEntity<Map<String, Object>> rejectUpdate(
            @PathVariable("requestId") Long requestId,
            @RequestBody(required = false) Map<String, String> body) {
        Map<String, Object> response = new HashMap<>();
        try {
            String comment = body != null ? body.get("comment") : null;
            metricService.rejectUpdate(requestId, comment);
            response.put("success", true);
            response.put("message", "已驳回变更申请");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
}
