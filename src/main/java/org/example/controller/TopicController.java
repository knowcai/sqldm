package org.example.controller;

import lombok.RequiredArgsConstructor;
import org.example.entity.Topic;
import org.example.service.TopicService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/topics")
@RequiredArgsConstructor
public class TopicController {

    private final TopicService topicService;

    /**
     * 获取所有主题
     */
    @GetMapping
    public ResponseEntity<?> getAllTopics() {
        try {
            List<Topic> topics = topicService.getAllTopics();
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", topics
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * 创建主题
     */
    @PostMapping
    public ResponseEntity<?> createTopic(@RequestBody Topic topic) {
        try {
            Topic created = topicService.createTopic(topic);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", created,
                    "message", "主题创建成功"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * 更新主题
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateTopic(@PathVariable Long id, @RequestBody Topic topic) {
        try {
            Topic updated = topicService.updateTopic(id, topic);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", updated,
                    "message", "主题更新成功"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * 删除主题
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteTopic(@PathVariable Long id) {
        try {
            topicService.deleteTopic(id);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "主题删除成功"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }
}
