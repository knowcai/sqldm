package org.example.service;

import lombok.RequiredArgsConstructor;
import org.example.entity.SysUser;
import org.example.entity.UserTopic;
import org.example.repository.SysUserRepository;
import org.example.repository.UserTopicRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final SysUserRepository userRepository;
    private final UserTopicRepository userTopicRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * 创建用户
     */
    @Transactional
    public SysUser createUser(SysUser user) {
        // 检查用户名是否已存在
        if (userRepository.findByUsernameAndIsDeletedFalse(user.getUsername()).isPresent()) {
            throw new RuntimeException("用户名已存在: " + user.getUsername());
        }
        
        // 加密密码
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setIsDeleted(false);
        user.setIsActive(true);
        
        return userRepository.save(user);
    }

    /**
     * 更新用户
     */
    @Transactional
    public SysUser updateUser(Long id, SysUser user) {
        SysUser existing = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("用户不存在: " + id));

        // 检查新用户名是否与其他用户冲突
        if (!existing.getUsername().equals(user.getUsername())) {
            if (userRepository.findByUsernameAndIsDeletedFalse(user.getUsername()).isPresent()) {
                throw new RuntimeException("用户名已存在: " + user.getUsername());
            }
        }

        existing.setUsername(user.getUsername());
        existing.setRealName(user.getRealName());
        existing.setEmail(user.getEmail());
        existing.setRole(user.getRole());
        existing.setIsActive(user.getIsActive());

        // 如果提供了新密码,则更新密码
        if (user.getPassword() != null && !user.getPassword().isEmpty()) {
            existing.setPassword(passwordEncoder.encode(user.getPassword()));
        }

        return userRepository.save(existing);
    }

    /**
     * 删除用户（逻辑删除）
     */
    @Transactional
    public void deleteUser(Long id) {
        SysUser user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("用户不存在: " + id));
        
        user.setIsDeleted(true);
        userRepository.save(user);
    }

    /**
     * 获取所有未删除的用户
     */
    public List<SysUser> getAllUsers() {
        return userRepository.findByIsDeletedFalseOrderByCreatedTimeDesc();
    }

    /**
     * 根据ID获取用户
     */
    public SysUser getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("用户不存在: " + id));
    }

    /**
     * 根据用户名获取用户
     */
    public SysUser getUserByUsername(String username) {
        return userRepository.findByUsernameAndIsDeletedFalse(username)
                .orElseThrow(() -> new RuntimeException("用户不存在: " + username));
    }

    /**
     * 为用户分配主题
     */
    @Transactional
    public UserTopic assignUserToTopic(Long userId, Long topicId, String role) {
        // 检查是否已经存在
        if (userTopicRepository.findByUserIdAndTopicId(userId, topicId).isPresent()) {
            throw new RuntimeException("用户已经分配到该主题");
        }

        UserTopic userTopic = new UserTopic();
        userTopic.setUserId(userId);
        userTopic.setTopicId(topicId);
        userTopic.setRole(role);

        return userTopicRepository.save(userTopic);
    }

    /**
     * 更新用户在主题中的角色
     */
    @Transactional
    public UserTopic updateUserTopicRole(Long userId, Long topicId, String role) {
        UserTopic userTopic = userTopicRepository.findByUserIdAndTopicId(userId, topicId)
                .orElseThrow(() -> new RuntimeException("用户未分配到该主题"));

        userTopic.setRole(role);
        return userTopicRepository.save(userTopic);
    }

    /**
     * 从主题中移除用户
     */
    @Transactional
    public void removeUserFromTopic(Long userId, Long topicId) {
        UserTopic userTopic = userTopicRepository.findByUserIdAndTopicId(userId, topicId)
                .orElseThrow(() -> new RuntimeException("用户未分配到该主题"));

        userTopicRepository.delete(userTopic);
    }

    /**
     * 获取用户在所有主题中的分配
     */
    public List<UserTopic> getUserTopics(Long userId) {
        return userTopicRepository.findByUserId(userId);
    }

    /**
     * 获取主题下的所有用户
     */
    public List<UserTopic> getTopicUsers(Long topicId) {
        return userTopicRepository.findByTopicId(topicId);
    }

    /**
     * 检查用户是否是主题管理员
     */
    public boolean isTopicAdmin(Long userId, Long topicId) {
        return userTopicRepository.findByUserIdAndTopicId(userId, topicId)
                .map(userTopic -> "ADMIN".equals(userTopic.getRole()))
                .orElse(false);
    }
}
