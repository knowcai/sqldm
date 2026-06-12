package org.example.controller;

import lombok.RequiredArgsConstructor;
import org.example.entity.SysUser;
import org.example.service.TopicApprovalService;
import org.example.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserDetailsService userDetailsService;
    private final UserService userService;
    private final AuthenticationManager authenticationManager;
    private final TopicApprovalService topicApprovalService;

    /**
     * 用户登录
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> credentials, HttpSession session) {
        try {
            String username = credentials.get("username");
            String password = credentials.get("password");

            if (username == null || password == null) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "用户名和密码不能为空"
                ));
            }

            // 使用AuthenticationManager验证密码
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, password)
            );
            
            // 将认证信息保存到SecurityContext
            SecurityContextHolder.getContext().setAuthentication(authentication);
            
            // 将认证信息保存到Session中
            session.setAttribute("SPRING_SECURITY_CONTEXT", SecurityContextHolder.getContext());

            // 获取用户信息
            SysUser user = userService.getUserByUsername(username);
            
            Map<String, Object> userInfo = new HashMap<>();
            userInfo.put("id", user.getId());
            userInfo.put("username", user.getUsername());
            userInfo.put("realName", user.getRealName());
            userInfo.put("role", user.getRole());
            userInfo.put("authorities", authentication.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .collect(Collectors.toList()));

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", userInfo,
                    "message", "登录成功"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "登录失败: " + e.getMessage()
            ));
        }
    }

    /**
     * 获取当前登录用户信息
     */
    @GetMapping("/current")
    public ResponseEntity<?> getCurrentUser() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "未登录"
                ));
            }

            String username = authentication.getName();
            SysUser user = userService.getUserByUsername(username);
            
            Map<String, Object> userInfo = new HashMap<>();
            userInfo.put("id", user.getId());
            userInfo.put("username", user.getUsername());
            userInfo.put("realName", user.getRealName());
            userInfo.put("role", user.getRole());
            userInfo.put("canApprove", !topicApprovalService.getApprovableTopicIds(user).isEmpty());
            userInfo.put("canDeleteMetric", topicApprovalService.isSuperAdmin(user)
                    || topicApprovalService.isTopicApprover(user));
            userInfo.put("canAccessAdmin", topicApprovalService.canAccessAdmin(user));
            userInfo.put("isTopicApprover", topicApprovalService.isTopicApprover(user));
            userInfo.put("approvableTopicIds", topicApprovalService.getApprovableTopicIds(user));
            userInfo.put("roleHint", buildRoleHint(user));

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", userInfo
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * 用户登出
     */
    @PostMapping("/logout")
    public ResponseEntity<?> logout() {
        SecurityContextHolder.clearContext();
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "登出成功"
        ));
    }

    private String buildRoleHint(SysUser user) {
        if (topicApprovalService.isSuperAdmin(user)) {
            return "超级管理员，可审批全部主题";
        }
        if (topicApprovalService.isTopicApprover(user)) {
            return "主题审批员（UserTopic.ADMIN），可审批所负责主题";
        }
        if ("TOPIC_ADMIN".equals(user.getRole())) {
            return "系统角色「主题管理员」≠ 主题审批员，需在用户-主题分配中设为「主题审批员」方可审批";
        }
        return "普通用户，提交变更需主题审批员审核";
    }
}
