package org.sqldm.service;

import lombok.RequiredArgsConstructor;
import org.sqldm.entity.SysUser;
import org.sqldm.entity.Topic;
import org.sqldm.entity.UserTopic;
import org.sqldm.repository.SysUserRepository;
import org.sqldm.repository.TopicRepository;
import org.sqldm.repository.UserTopicRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TopicService {

    private final TopicRepository topicRepository;
    private final UserTopicRepository userTopicRepository;
    private final SysUserRepository userRepository;
    private final TopicApprovalService topicApprovalService;
    private final CurrentUserService currentUserService;

    /**
     * 创建主题
     */
    @Transactional
    public Topic createTopic(Topic topic) {
        assertSuperAdminForTopicWrite();
        if (topicRepository.findByTopicNameAndIsDeletedFalse(topic.getTopicName()).isPresent()) {
            throw new RuntimeException("主题名称已存在: " + topic.getTopicName());
        }

        topic.setTopicCode(generateTopicCode());
        topic.setIsDeleted(false);
        return topicRepository.save(topic);
    }

    /**
     * 更新主题
     */
    @Transactional
    public Topic updateTopic(Long id, Topic topic) {
        assertSuperAdminForTopicWrite();
        Topic existing = topicRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("主题不存在: " + id));

        if (!existing.getTopicName().equals(topic.getTopicName())) {
            if (topicRepository.findByTopicNameAndIsDeletedFalse(topic.getTopicName()).isPresent()) {
                throw new RuntimeException("主题名称已存在: " + topic.getTopicName());
            }
        }

        existing.setTopicName(topic.getTopicName());
        existing.setDescription(topic.getDescription());
        existing.setAdminId(topic.getAdminId());

        return topicRepository.save(existing);
    }

    /**
     * 删除主题（逻辑删除）
     */
    @Transactional
    public void deleteTopic(Long id) {
        assertSuperAdminForTopicWrite();
        Topic topic = topicRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("主题不存在: " + id));
        
        topic.setIsDeleted(true);
        topicRepository.save(topic);
    }

    /**
     * 获取所有未删除的主题
     */
    public List<Topic> getAllTopics() {
        return topicRepository.findByIsDeletedFalseOrderByCreatedTimeDesc();
    }

    public List<Topic> getManageableTopics() {
        SysUser user = currentUserService.requireCurrentUser();
        if (topicApprovalService.isSuperAdmin(user)) {
            return getAllTopics();
        }
        List<Long> topicIds = topicApprovalService.getApprovableTopicIds(user);
        if (topicIds.isEmpty()) {
            return List.of();
        }
        return topicRepository.findByIsDeletedFalseOrderByCreatedTimeDesc().stream()
                .filter(t -> topicIds.contains(t.getId()))
                .toList();
    }

    /** 当前用户可用的主题（指标页下拉）：超级管理员全部，其他用户仅已分配主题 */
    public List<Topic> getMyTopics() {
        SysUser user = currentUserService.requireCurrentUser();
        if (topicApprovalService.isSuperAdmin(user)) {
            return getAllTopics();
        }
        List<Long> topicIds = userTopicRepository.findByUserId(user.getId()).stream()
                .map(UserTopic::getTopicId)
                .distinct()
                .toList();
        if (topicIds.isEmpty()) {
            return List.of();
        }
        return topicRepository.findByIsDeletedFalseOrderByCreatedTimeDesc().stream()
                .filter(t -> topicIds.contains(t.getId()))
                .toList();
    }

    /**
     * 根据ID获取主题
     */
    public Topic getTopicById(Long id) {
        return topicRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("主题不存在: " + id));
    }

    public List<Map<String, Object>> getTopicUsersDetail(Long topicId) {
        topicApprovalService.assertCanManageTopic(currentUserService.requireCurrentUser(), topicId);
        List<UserTopic> userTopics = userTopicRepository.findByTopicId(topicId);
        List<Map<String, Object>> rows = new ArrayList<>();
        for (UserTopic ut : userTopics) {
            SysUser user = userRepository.findById(ut.getUserId()).orElse(null);
            if (user == null) {
                continue;
            }
            Map<String, Object> row = new HashMap<>();
            row.put("id", ut.getId());
            row.put("userId", ut.getUserId());
            row.put("topicId", ut.getTopicId());
            row.put("role", ut.getRole());
            row.put("username", user.getUsername());
            row.put("realName", user.getRealName());
            row.put("email", user.getEmail());
            row.put("systemRole", user.getRole());
            row.put("isActive", user.getIsActive());
            rows.add(row);
        }
        return rows;
    }

    private String generateTopicCode() {
        return "topic_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }

    private void assertSuperAdminForTopicWrite() {
        SysUser user = currentUserService.requireCurrentUser();
        if (!topicApprovalService.isSuperAdmin(user)) {
            throw new RuntimeException("仅超级管理员可创建或修改主题");
        }
    }
}
