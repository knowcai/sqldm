package org.sqldm.controller;

import lombok.RequiredArgsConstructor;
import org.sqldm.entity.SysUser;
import org.sqldm.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/init")
@RequiredArgsConstructor
public class InitController {

    private final UserService userService;

    /**
     * 初始化默认用户
     * 注意: 此接口仅供初始化使用,生产环境应删除或禁用
     */
    @PostMapping("/users")
    public ResponseEntity<?> initUsers() {
        Map<String, Object> response = new HashMap<>();
        try {
            // 创建超级管理员
            SysUser superAdmin = new SysUser();
            superAdmin.setUsername("root");
            superAdmin.setPassword("root");
            superAdmin.setRealName("超级管理员");
            superAdmin.setRole("SUPER_ADMIN");
            userService.createUser(superAdmin);

            // 创建主题管理员1
            SysUser topicAdmin1 = new SysUser();
            topicAdmin1.setUsername("admin1");
            topicAdmin1.setPassword("admin1");
            topicAdmin1.setRealName("主题管理员1");
            topicAdmin1.setRole("TOPIC_ADMIN");
            userService.createUser(topicAdmin1);

            // 创建主题管理员2
            SysUser topicAdmin2 = new SysUser();
            topicAdmin2.setUsername("admin2");
            topicAdmin2.setPassword("admin2");
            topicAdmin2.setRealName("主题管理员2");
            topicAdmin2.setRole("TOPIC_ADMIN");
            userService.createUser(topicAdmin2);

            // 创建普通用户1
            SysUser user1 = new SysUser();
            user1.setUsername("user1");
            user1.setPassword("user1");
            user1.setRealName("普通用户1");
            user1.setRole("USER");
            userService.createUser(user1);

            // 创建普通用户2
            SysUser user2 = new SysUser();
            user2.setUsername("user2");
            user2.setPassword("user2");
            user2.setRealName("普通用户2");
            user2.setRole("USER");
            userService.createUser(user2);

            response.put("success", true);
            response.put("message", "默认用户初始化成功");
            response.put("users", new String[]{
                "超级管理员: root/root",
                "主题管理员1: admin1/admin1",
                "主题管理员2: admin2/admin2",
                "普通用户1: user1/user1",
                "普通用户2: user2/user2"
            });
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "初始化失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
}
