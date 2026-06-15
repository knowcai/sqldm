package org.sqldm.service;

import lombok.RequiredArgsConstructor;
import org.sqldm.entity.SysUser;
import org.sqldm.entity.UserTopic;
import org.sqldm.repository.TopicRepository;
import org.sqldm.repository.UserTopicRepository;
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

    /** 系统管理页：超级管理员全功能；主题审批员可管理主题成员 */
    public boolean canAccessAdmin(SysUser user) {
        return isSuperAdmin(user) || isTopicApprover(user);
    }

    public boolean isFullAdmin(SysUser user) {
        return isSuperAdmin(user);
    }

    /** 是否在任一主题担任审批员（UserTopic.ADMIN） */
    public boolean isTopicApprover(SysUser user) {
        return user != null && !getApprovableTopicIds(user).isEmpty() && !isSuperAdmin(user);
    }

    /** 是否可管理主题下的用户（分配/移除/编辑成员） */
    public boolean canManageTopicUsers(SysUser user) {
        return isSuperAdmin(user) || isTopicApprover(user);
    }

    public void assertCanManageTopic(SysUser user, Long topicId) {
        if (!canApproveTopic(user, topicId)) {
            throw new RuntimeException("无权管理该主题下的用户");
        }
    }

    /** 主题审批员仅可管理主题内 MEMBER；超级管理员可管理 ADMIN 与 MEMBER */
    public boolean canManageTopicUserRole(SysUser operator, Long topicId, String topicUserRole) {
        if (operator == null || topicId == null) {
            return false;
        }
        if (isSuperAdmin(operator)) {
            return true;
        }
        if (!"MEMBER".equals(topicUserRole)) {
            return false;
        }
        return canApproveTopic(operator, topicId);
    }

    public void assertCanManageTopicUserRole(SysUser operator, Long topicId, String topicUserRole) {
        if (!canManageTopicUserRole(operator, topicId, topicUserRole)) {
            if ("ADMIN".equals(topicUserRole)) {
                throw new RuntimeException("仅超级管理员可管理主题审批员");
            }
            throw new RuntimeException("无权管理该主题下的用户");
        }
    }

    /** 是否可在主题内分配指定角色（新增用户到主题时） */
    public boolean canAssignTopicRole(SysUser operator, Long topicId, String roleToAssign) {
        if (isSuperAdmin(operator)) {
            return canApproveTopic(operator, topicId) || isSuperAdmin(operator);
        }
        if (!canApproveTopic(operator, topicId)) {
            return false;
        }
        return "MEMBER".equals(roleToAssign);
    }

    public void assertCanAssignTopicRole(SysUser operator, Long topicId, String roleToAssign) {
        if (isSuperAdmin(operator)) {
            return;
        }
        if (!canApproveTopic(operator, topicId)) {
            throw new RuntimeException("无权管理该主题下的用户");
        }
        if (!"MEMBER".equals(roleToAssign)) {
            throw new RuntimeException("仅超级管理员可任命主题审批员");
        }
    }
}
