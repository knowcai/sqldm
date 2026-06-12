package org.example.controller;

import lombok.RequiredArgsConstructor;
import org.example.service.ApiAccessLogService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/access-logs")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ApiAccessLogController {

    private final ApiAccessLogService apiAccessLogService;

    @GetMapping
    public ResponseEntity<Map<String, Object>> listRecent() {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", apiAccessLogService.listRecent());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> stats() {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", apiAccessLogService.stats());
        return ResponseEntity.ok(response);
    }
}
