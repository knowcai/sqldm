package org.example.controller;

import lombok.RequiredArgsConstructor;
import org.example.entity.SysUser;
import org.example.entity.UserTopic;
import org.example.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * 获取所有用户
     */
    @GetMapping
    public ResponseEntity<?> getAllUsers() {
        try {
            List<SysUser> users = userService.getAllUsers();
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", users
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * 创建用户
     */
    @PostMapping
    public ResponseEntity<?> createUser(@RequestBody SysUser user) {
        try {
            SysUser created = userService.createUser(user);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", created,
                    "message", "用户创建成功"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * 更新用户
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateUser(@PathVariable Long id, @RequestBody SysUser user) {
        try {
            SysUser updated = userService.updateUser(id, user);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", updated,
                    "message", "用户更新成功"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * 删除用户
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {
        try {
            userService.deleteUser(id);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "用户删除成功"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * 为用户分配主题
     */
    @PostMapping("/{userId}/topics/{topicId}")
    public ResponseEntity<?> assignUserToTopic(
            @PathVariable Long userId,
            @PathVariable Long topicId,
            @RequestParam String role) {
        try {
            UserTopic userTopic = userService.assignUserToTopic(userId, topicId, role);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", userTopic,
                    "message", "分配成功"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * 获取用户在所有主题中的分配
     */
    @GetMapping("/{userId}/topics")
    public ResponseEntity<?> getUserTopics(@PathVariable Long userId) {
        try {
            List<UserTopic> userTopics = userService.getUserTopics(userId);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", userTopics
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * 从主题中移除用户
     */
    @DeleteMapping("/{userId}/topics/{topicId}")
    public ResponseEntity<?> removeUserFromTopic(
            @PathVariable Long userId,
            @PathVariable Long topicId) {
        try {
            userService.removeUserFromTopic(userId, topicId);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "移除成功"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }
}
