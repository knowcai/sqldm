package org.example.service;

import lombok.RequiredArgsConstructor;
import org.example.entity.SysUser;
import org.example.entity.UserTopic;
import org.example.repository.TopicRepository;
import org.example.repository.UserTopicRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TopicApprovalService {

    private final UserTopicRepository userTopicRepository;
    private final TopicRepository topicRepository;

    public boolean isSuperAdmin(SysUser user) {
        return user != null && "SUPER_ADMIN".equals(user.getRole());
    }

    /** 是否可审批该主题（主题 ADMIN 或超级管理员） */
    public boolean canApproveTopic(SysUser user, Long topicId) {
        if (user == null || topicId == null) {
            return false;
        }
        if (isSuperAdmin(user)) {
            return true;
        }
        return userTopicRepository.findByUserIdAndTopicId(user.getId(), topicId)
                .map(ut -> "ADMIN".equals(ut.getRole()))
                .orElse(false);
    }

    /** 当前用户可审批的主题 ID 列表（按主题维度，新任命管理员也能看到历史待审） */
    public List<Long> getApprovableTopicIds(SysUser user) {
        if (user == null) {
            return List.of();
        }
        if (isSuperAdmin(user)) {
            return topicRepository.findAll().stream()
                    .map(t -> t.getId())
                    .collect(Collectors.toList());
        }
        return userTopicRepository.findByUserIdAndRole(user.getId(), "ADMIN").stream()
                .map(UserTopic::getTopicId)
                .distinct()
                .collect(Collectors.toList());
    }

    /** 提交指标变更是否需要走审批 */
    public boolean requiresApproval(SysUser user, Long topicId) {
        return !canApproveTopic(user, topicId);
    }

    public void assertCanApprove(SysUser user, Long topicId) {
        if (!canApproveTopic(user, topicId)) {
            throw new RuntimeException("无权审批该主题的指标");
        }
    }

    /** 是否可删除该主题下的指标（超级管理员或主题审批员） */
    public boolean canDeleteMetric(SysUser user, Long topicId) {
        if (user == null || topicId == null) {
            return false;
        }
        return canApproveTopic(user, topicId);
    }

    /** 系统管理页仅超级管理员可见 */
    public boolean canAccessAdmin(SysUser user) {
        return isSuperAdmin(user);
    }

    /** 是否在任一主题担任审批员（UserTopic.ADMIN） */
    public boolean isTopicApprover(SysUser user) {
        return user != null && !getApprovableTopicIds(user).isEmpty() && !isSuperAdmin(user);
    }
}
