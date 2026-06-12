package org.example.controller;

import lombok.RequiredArgsConstructor;
import org.example.entity.MetricTag;
import org.example.service.MetricTagService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tags")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class MetricTagController {

    private final MetricTagService metricTagService;

    @GetMapping
    public ResponseEntity<Map<String, Object>> listTags() {
        Map<String, Object> response = new HashMap<>();
        List<MetricTag> tags = metricTagService.listAllTags();
        response.put("success", true);
        response.put("data", tags);
        response.put("total", tags.size());
        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> createTag(@RequestBody Map<String, String> body) {
        Map<String, Object> response = new HashMap<>();
        try {
            MetricTag tag = metricTagService.createTag(body.get("tagName"));
            response.put("success", true);
            response.put("data", tag);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
}
