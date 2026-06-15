package org.sqldm.controller;

import lombok.RequiredArgsConstructor;
import org.sqldm.dto.UserSaveRequest;
import org.sqldm.entity.SysUser;
import org.sqldm.entity.UserTopic;
import org.sqldm.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    public ResponseEntity<?> getAllUsers() {
        try {
            List<SysUser> users = userService.getManageableUsers();
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

    @GetMapping("/{id}")
    public ResponseEntity<?> getUserById(@PathVariable Long id) {
        try {
            Map<String, Object> detail = userService.getUserDetail(id);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", detail
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    @PostMapping
    public ResponseEntity<?> createUser(@RequestBody UserSaveRequest request) {
        try {
            SysUser created = userService.createUserWithTopics(request);
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

    @PutMapping("/{id}")
    public ResponseEntity<?> updateUser(@PathVariable Long id, @RequestBody UserSaveRequest request) {
        try {
            SysUser updated = userService.updateUserWithTopics(id, request);
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

    @PutMapping("/{userId}/topics/{topicId}")
    public ResponseEntity<?> updateUserTopicRole(
            @PathVariable Long userId,
            @PathVariable Long topicId,
            @RequestParam String role) {
        try {
            UserTopic userTopic = userService.updateUserTopicRole(userId, topicId, role);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", userTopic,
                    "message", "角色更新成功"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

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
