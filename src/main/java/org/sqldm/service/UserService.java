package org.sqldm.service;

import lombok.RequiredArgsConstructor;
import org.sqldm.dto.UserSaveRequest;
import org.sqldm.dto.UserTopicAssignmentDto;
import org.sqldm.entity.SysUser;
import org.sqldm.entity.UserTopic;
import org.sqldm.repository.SysUserRepository;
import org.sqldm.repository.TopicRepository;
import org.sqldm.repository.UserTopicRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final SysUserRepository userRepository;
    private final UserTopicRepository userTopicRepository;
    private final TopicRepository topicRepository;
    private final PasswordEncoder passwordEncoder;
    private final TopicApprovalService topicApprovalService;
    private final CurrentUserService currentUserService;

    /**
     * 创建用户（仅超级管理员）
     */
    @Transactional
    public SysUser createUser(SysUser user) {
        SysUser operator = currentUserService.requireCurrentUser();
        if (!topicApprovalService.isSuperAdmin(operator)) {
            throw new RuntimeException("仅超级管理员可创建用户");
        }
        if (userRepository.findByUsername(user.getUsername()).isPresent()) {
            throw new RuntimeException("用户名已存在: " + user.getUsername());
        }
        if (user.getPassword() == null || user.getPassword().isBlank()) {
            throw new RuntimeException("密码不能为空");
        }
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        if (user.getIsActive() == null) {
            user.setIsActive(true);
        }
        return userRepository.save(user);
    }

    @Transactional
    public SysUser createUserWithTopics(UserSaveRequest request) {
        SysUser user = new SysUser();
        user.setUsername(request.getUsername());
        user.setPassword(request.getPassword());
        user.setRealName(request.getRealName());
        user.setEmail(request.getEmail());
        user.setRole(request.getRole());
        user.setIsActive(request.getIsActive() != null ? request.getIsActive() : true);
        SysUser saved = createUser(user);
        syncUserTopics(saved.getId(), request.getTopicAssignments());
        return saved;
    }

    @Transactional
    public SysUser updateUserWithTopics(Long id, UserSaveRequest request) {
        SysUser operator = currentUserService.requireCurrentUser();
        if (topicApprovalService.isSuperAdmin(operator)) {
            SysUser user = new SysUser();
            user.setUsername(request.getUsername());
            user.setPassword(request.getPassword());
            user.setRealName(request.getRealName());
            user.setEmail(request.getEmail());
            user.setRole(request.getRole());
            user.setIsActive(request.getIsActive());
            SysUser saved = updateUser(id, user);
            if (request.getTopicAssignments() != null) {
                syncUserTopics(id, request.getTopicAssignments());
            }
            return saved;
        }
        if (topicApprovalService.isTopicApprover(operator)) {
            SysUser target = userRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("用户不存在: " + id));
            assertCanEditUser(operator, target);
            syncUserTopicsForApprover(id, request.getTopicAssignments(), operator);
            return target;
        }
        throw new RuntimeException("无权编辑用户");
    }

    public Map<String, Object> getUserDetail(Long id) {
        SysUser user = getUserById(id);
        Map<String, Object> detail = new java.util.HashMap<>();
        detail.put("user", user);
        detail.put("topicAssignments", getUserTopicAssignments(id));
        return detail;
    }

    public List<UserTopicAssignmentDto> getUserTopicAssignments(Long userId) {
        return userTopicRepository.findByUserId(userId).stream().map(ut -> {
            UserTopicAssignmentDto dto = new UserTopicAssignmentDto();
            dto.setTopicId(ut.getTopicId());
            dto.setRole(ut.getRole());
            topicRepository.findById(ut.getTopicId()).ifPresent(t -> dto.setTopicName(t.getTopicName()));
            return dto;
        }).toList();
    }

    @Transactional
    public void syncUserTopics(Long userId, List<UserTopicAssignmentDto> assignments) {
        SysUser operator = currentUserService.requireCurrentUser();
        if (!topicApprovalService.isSuperAdmin(operator)) {
            throw new RuntimeException("仅超级管理员可分配主题");
        }
        SysUser target = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在: " + userId));
        if ("SUPER_ADMIN".equals(target.getRole())) {
            return;
        }
        List<UserTopicAssignmentDto> safe = assignments == null ? List.of() : assignments;
        String topicRole = resolveTopicRoleForUser(target);
        Set<Long> newTopicIds = new HashSet<>();
        for (UserTopicAssignmentDto a : safe) {
            if (a.getTopicId() == null) {
                continue;
            }
            topicApprovalService.assertCanAssignTopicRole(operator, a.getTopicId(), topicRole);
            newTopicIds.add(a.getTopicId());
            userTopicRepository.findByUserIdAndTopicId(userId, a.getTopicId())
                    .ifPresentOrElse(ut -> {
                        ut.setRole(topicRole);
                        userTopicRepository.save(ut);
                    }, () -> {
                        UserTopic ut = new UserTopic();
                        ut.setUserId(userId);
                        ut.setTopicId(a.getTopicId());
                        ut.setRole(topicRole);
                        userTopicRepository.save(ut);
                    });
        }
        for (UserTopic existing : userTopicRepository.findByUserId(userId)) {
            if (!newTopicIds.contains(existing.getTopicId())) {
                userTopicRepository.delete(existing);
            }
        }
    }

    /**
     * 主题审批员：仅可在负责的主题下为普通用户分配/移除「成员」，不能授予审批权限。
     * 用户在其他主题上的分配保持不变。
     */
    @Transactional
    public void syncUserTopicsForApprover(Long userId, List<UserTopicAssignmentDto> assignments,
                                          SysUser operator) {
        SysUser target = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在: " + userId));
        if (!"USER".equals(target.getRole())) {
            throw new RuntimeException("仅可为普通用户分配主题");
        }
        List<Long> approvableTopicIds = topicApprovalService.getApprovableTopicIds(operator);
        if (approvableTopicIds.isEmpty()) {
            throw new RuntimeException("无权分配主题");
        }
        Set<Long> approvableSet = new HashSet<>(approvableTopicIds);
        List<UserTopicAssignmentDto> safe = assignments == null ? List.of() : assignments;
        Set<Long> selectedOnApprovable = new HashSet<>();
        for (UserTopicAssignmentDto a : safe) {
            if (a.getTopicId() == null || !approvableSet.contains(a.getTopicId())) {
                continue;
            }
            selectedOnApprovable.add(a.getTopicId());
            userTopicRepository.findByUserIdAndTopicId(userId, a.getTopicId())
                    .ifPresentOrElse(ut -> {
                        ut.setRole("MEMBER");
                        userTopicRepository.save(ut);
                    }, () -> {
                        UserTopic ut = new UserTopic();
                        ut.setUserId(userId);
                        ut.setTopicId(a.getTopicId());
                        ut.setRole("MEMBER");
                        userTopicRepository.save(ut);
                    });
        }
        for (UserTopic existing : userTopicRepository.findByUserId(userId)) {
            if (approvableSet.contains(existing.getTopicId())
                    && !selectedOnApprovable.contains(existing.getTopicId())) {
                userTopicRepository.delete(existing);
            }
        }
    }

    /**
     * 更新用户
     */
    @Transactional
    public SysUser updateUser(Long id, SysUser user) {
        SysUser operator = currentUserService.requireCurrentUser();
        SysUser existing = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("用户不存在: " + id));
        assertCanEditUser(operator, existing);

        if (!topicApprovalService.isSuperAdmin(operator)) {
            if (!"USER".equals(existing.getRole())) {
                throw new RuntimeException("仅超级管理员可编辑主题审批员");
            }
            user.setRole("USER");
        } else if (user.getRole() == null || user.getRole().isBlank()) {
            user.setRole(existing.getRole());
        }

        // root 账号角色不可变更（仍可改密码等信息）
        if (isProtectedRootUser(existing)) {
            user.setRole(existing.getRole());
        }

        // 检查新用户名是否与其他用户冲突
        if (!existing.getUsername().equals(user.getUsername())) {
            if (userRepository.findByUsername(user.getUsername()).isPresent()) {
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
     * 删除用户（物理删除，仅超级管理员）
     */
    @Transactional
    public void deleteUser(Long id) {
        SysUser operator = currentUserService.requireCurrentUser();
        if (!topicApprovalService.isSuperAdmin(operator)) {
            throw new RuntimeException("仅超级管理员可删除用户");
        }
        SysUser user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("用户不存在: " + id));

        if (isProtectedRootUser(user)) {
            throw new RuntimeException("不能删除 root 超级管理员");
        }

        userTopicRepository.findByUserId(id).forEach(userTopicRepository::delete);
        userRepository.delete(user);
    }

    /**
     * 获取当前操作者可管理的用户
     */
    public List<SysUser> getManageableUsers() {
        SysUser operator = currentUserService.requireCurrentUser();
        if (topicApprovalService.isSuperAdmin(operator)) {
            return getAllUsers();
        }
        if (topicApprovalService.isTopicApprover(operator)) {
            return userRepository.findAllByOrderByCreatedTimeDesc().stream()
                    .filter(u -> "USER".equals(u.getRole()))
                    .collect(Collectors.toList());
        }
        return List.of();
    }

    /**
     * 获取所有用户
     */
    public List<SysUser> getAllUsers() {
        return userRepository.findAllByOrderByCreatedTimeDesc();
    }

    /**
     * 根据ID获取用户
     */
    public SysUser getUserById(Long id) {
        SysUser user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("用户不存在: " + id));
        try {
            SysUser operator = currentUserService.requireCurrentUser();
            if (!topicApprovalService.isSuperAdmin(operator)) {
                assertCanEditUser(operator, user);
            }
        } catch (Exception ignored) {
            // Init 等无会话场景保持兼容
        }
        return user;
    }

    /**
     * 根据用户名获取用户
     */
    public SysUser getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在: " + username));
    }

    /**
     * 为用户分配主题
     */
    @Transactional
    public UserTopic assignUserToTopic(Long userId, Long topicId, String role) {
        SysUser operator = currentUserService.requireCurrentUser();
        topicApprovalService.assertCanAssignTopicRole(operator, topicId, role);

        SysUser target = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在: " + userId));
        if ("SUPER_ADMIN".equals(target.getRole())) {
            throw new RuntimeException("不能将超级管理员分配到主题");
        }
        if (!topicApprovalService.isSuperAdmin(operator) && !"USER".equals(target.getRole())) {
            throw new RuntimeException("仅超级管理员可将主题审批员加入主题");
        }
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
        SysUser operator = currentUserService.requireCurrentUser();
        topicApprovalService.assertCanAssignTopicRole(operator, topicId, role);

        UserTopic userTopic = userTopicRepository.findByUserIdAndTopicId(userId, topicId)
                .orElseThrow(() -> new RuntimeException("用户未分配到该主题"));
        topicApprovalService.assertCanManageTopicUserRole(operator, topicId, userTopic.getRole());
        if (!topicApprovalService.isSuperAdmin(operator) && "ADMIN".equals(userTopic.getRole())) {
            throw new RuntimeException("仅超级管理员可调整主题审批员角色");
        }

        userTopic.setRole(role);
        return userTopicRepository.save(userTopic);
    }

    /**
     * 从主题中移除用户
     */
    @Transactional
    public void removeUserFromTopic(Long userId, Long topicId) {
        SysUser operator = currentUserService.requireCurrentUser();
        topicApprovalService.assertCanManageTopic(operator, topicId);

        UserTopic userTopic = userTopicRepository.findByUserIdAndTopicId(userId, topicId)
                .orElseThrow(() -> new RuntimeException("用户未分配到该主题"));
        topicApprovalService.assertCanManageTopicUserRole(operator, topicId, userTopic.getRole());

        userTopicRepository.delete(userTopic);
    }

    /**
     * 获取主题下的所有用户（含用户详情）
     */
    public List<UserTopic> getTopicUsersWithPermission(Long topicId) {
        SysUser operator = currentUserService.requireCurrentUser();
        topicApprovalService.assertCanManageTopic(operator, topicId);
        return userTopicRepository.findByTopicId(topicId);
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

    private boolean isProtectedRootUser(SysUser user) {
        return user != null && "root".equals(user.getUsername());
    }

    /** 主题内角色由系统角色决定：主题管理员=审批员，普通用户=成员 */
    private String resolveTopicRoleForUser(SysUser user) {
        if (user != null && "TOPIC_ADMIN".equals(user.getRole())) {
            return "ADMIN";
        }
        return "MEMBER";
    }

    private void assertCanEditUser(SysUser operator, SysUser target) {
        if (topicApprovalService.isSuperAdmin(operator)) {
            return;
        }
        if (topicApprovalService.isTopicApprover(operator)) {
            if (!"USER".equals(target.getRole())) {
                throw new RuntimeException("仅超级管理员可编辑该用户");
            }
            return;
        }
        throw new RuntimeException("无权编辑该用户");
    }
}
