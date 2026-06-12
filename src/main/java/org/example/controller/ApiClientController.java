package org.example.controller;

import lombok.RequiredArgsConstructor;
import org.example.entity.ApiClient;
import org.example.service.ApiClientService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/api-clients")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ApiClientController {

    private final ApiClientService apiClientService;

    @GetMapping
    public ResponseEntity<Map<String, Object>> listAll() {
        Map<String, Object> response = new HashMap<>();
        List<ApiClient> list = apiClientService.listAll();
        response.put("success", true);
        response.put("data", list);
        response.put("total", list.size());
        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> create(@RequestBody ApiClient client) {
        Map<String, Object> response = new HashMap<>();
        try {
            ApiClient created = apiClientService.create(client);
            response.put("success", true);
            response.put("data", created);
            response.put("message", "API 客户端创建成功");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> update(@PathVariable("id") Long id,
                                                      @RequestBody ApiClient client) {
        Map<String, Object> response = new HashMap<>();
        try {
            ApiClient updated = apiClientService.update(id, client);
            response.put("success", true);
            response.put("data", updated);
            response.put("message", "API 客户端更新成功");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/{id}/regenerate-key")
    public ResponseEntity<Map<String, Object>> regenerateKey(@PathVariable("id") Long id) {
        Map<String, Object> response = new HashMap<>();
        try {
            ApiClient updated = apiClientService.regenerateKey(id);
            response.put("success", true);
            response.put("data", updated);
            response.put("message", "API Key 已重新生成");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
}
