package org.example.controller;

import lombok.RequiredArgsConstructor;
import org.example.entity.WebhookConfig;
import org.example.service.WebhookConfigService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/webhooks")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class WebhookController {

    private final WebhookConfigService webhookConfigService;

    @GetMapping
    public ResponseEntity<Map<String, Object>> listAll() {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", webhookConfigService.listAll());
        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> create(@RequestBody WebhookConfig config) {
        Map<String, Object> response = new HashMap<>();
        try {
            response.put("success", true);
            response.put("data", webhookConfigService.create(config));
            response.put("message", "Webhook 创建成功");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> update(@PathVariable("id") Long id,
                                                      @RequestBody WebhookConfig config) {
        Map<String, Object> response = new HashMap<>();
        try {
            response.put("success", true);
            response.put("data", webhookConfigService.update(id, config));
            response.put("message", "Webhook 更新成功");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> delete(@PathVariable("id") Long id) {
        Map<String, Object> response = new HashMap<>();
        try {
            webhookConfigService.delete(id);
            response.put("success", true);
            response.put("message", "Webhook 已停用");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
}
