package org.sqldm.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.sqldm.dto.ApprovalItemDto;
import org.sqldm.dto.SubmissionItemDto;
import org.sqldm.entity.MetricApprovalRequest;
import org.sqldm.entity.MetricDefinition;
import org.sqldm.entity.SysUser;
import org.sqldm.repository.MetricApprovalRequestRepository;
import org.sqldm.repository.MetricRepository;
import org.sqldm.util.MetricDiffHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MetricApprovalService {

    public static final String STATUS_PENDING = "PENDING";
    public static final String TYPE_CREATE = "CREATE";
    public static final String TYPE_UPDATE = "UPDATE";
    public static final String TYPE_DELETE = "DELETE";

    private final MetricApprovalRequestRepository approvalRepository;
    private final MetricRepository metricRepository;
    private final TopicApprovalService topicApprovalService;
    private final CurrentUserService currentUserService;
    private final ObjectMapper objectMapper;

    public long countPendingForCurrentUser() {
        SysUser user = currentUserService.requireCurrentUser();
        List<Long> topicIds = topicApprovalService.getApprovableTopicIds(user);
        if (topicIds.isEmpty()) {
            return 0;
        }
        long updateCount = approvalRepository.countByStatusAndTopicIdIn(STATUS_PENDING, topicIds);
        long createCount = metricRepository
                .findByStatusAndTopicIdInAndIsDeletedFalseOrderByCreatedTimeDesc("PENDING_APPROVAL", topicIds)
                .size();
        long deleteCount = approvalRepository
                .findByStatusAndTopicIdInAndRequestTypeOrderByCreatedTimeDesc(STATUS_PENDING, topicIds, TYPE_DELETE)
                .size();
        return updateCount + createCount + deleteCount;
    }

    public boolean canCurrentUserApprove() {
        SysUser user = currentUserService.requireCurrentUser();
        return !topicApprovalService.getApprovableTopicIds(user).isEmpty();
    }

    public List<ApprovalItemDto> listPendingForCurrentUser() {
        SysUser user = currentUserService.requireCurrentUser();
        List<Long> topicIds = topicApprovalService.getApprovableTopicIds(user);
        if (topicIds.isEmpty()) {
            return List.of();
        }

        List<ApprovalItemDto> items = new ArrayList<>();

        for (MetricDefinition metric : metricRepository
                .findByStatusAndTopicIdInAndIsDeletedFalseOrderByCreatedTimeDesc("PENDING_APPROVAL", topicIds)) {
            ApprovalItemDto dto = new ApprovalItemDto();
            dto.setRequestType(TYPE_CREATE);
            dto.setMetricId(metric.getId());
            dto.setTopicId(metric.getTopicId());
            dto.setTopicName(metric.getTopicName());
            dto.setMetricName(metric.getMetricName());
            dto.setMetricCode(metric.getMetricCode());
            dto.setSubmittedBy(metric.getCreatedBy());
            dto.setSubmittedTime(metric.getCreatedTime());
            dto.setPreview(metric);
            items.add(dto);
        }

        for (MetricApprovalRequest req : approvalRepository
                .findByStatusAndTopicIdInOrderByCreatedTimeDesc(STATUS_PENDING, topicIds)) {
            if (TYPE_UPDATE.equals(req.getRequestType())) {
                MetricDefinition existing = metricRepository.findById(req.getMetricId()).orElse(null);
                if (existing == null) {
                    continue;
                }
                ApprovalItemDto dto = new ApprovalItemDto();
                dto.setRequestType(TYPE_UPDATE);
                dto.setRequestId(req.getId());
                dto.setMetricId(req.getMetricId());
                dto.setTopicId(req.getTopicId());
                dto.setTopicName(existing.getTopicName());
                dto.setSubmittedBy(req.getSubmittedBy());
                dto.setSubmittedTime(req.getCreatedTime());
                dto.setCurrent(existing);
                try {
                    MetricDefinition preview = objectMapper.readValue(req.getPayload(), MetricDefinition.class);
                    dto.setMetricName(preview.getMetricName());
                    dto.setMetricCode(preview.getMetricCode());
                    dto.setPreview(preview);
                    dto.setChangedFields(MetricDiffHelper.diffFields(existing, preview));
                } catch (Exception e) {
                    dto.setMetricName(existing.getMetricName());
                    dto.setMetricCode(existing.getMetricCode());
                    dto.setPreview(existing);
                }
                items.add(dto);
            } else if (TYPE_DELETE.equals(req.getRequestType())) {
                MetricDefinition existing = metricRepository.findById(req.getMetricId()).orElse(null);
                if (existing == null || Boolean.TRUE.equals(existing.getIsDeleted())) {
                    continue;
                }
                ApprovalItemDto dto = new ApprovalItemDto();
                dto.setRequestType(TYPE_DELETE);
                dto.setRequestId(req.getId());
                dto.setMetricId(req.getMetricId());
                dto.setTopicId(req.getTopicId());
                dto.setTopicName(existing.getTopicName());
                dto.setMetricName(existing.getMetricName());
                dto.setMetricCode(existing.getMetricCode());
                dto.setSubmittedBy(req.getSubmittedBy());
                dto.setSubmittedTime(req.getCreatedTime());
                dto.setPreview(existing);
                items.add(dto);
            }
        }

        items.sort(Comparator.comparing(ApprovalItemDto::getSubmittedTime,
                Comparator.nullsLast(Comparator.reverseOrder())));
        return items;
    }

    @Transactional
    public void approveCreate(Long metricId, String comment) {
        SysUser user = currentUserService.requireCurrentUser();
        MetricDefinition metric = metricRepository.findById(metricId)
                .orElseThrow(() -> new RuntimeException("指标不存在"));
        if (!"PENDING_APPROVAL".equals(metric.getStatus())) {
            throw new RuntimeException("该指标不在待审批状态");
        }
        topicApprovalService.assertCanApprove(user, metric.getTopicId());
        metric.setStatus("ACTIVE");
        metric.setUpdatedBy(user.getUsername());
        metricRepository.save(metric);
    }

    @Transactional
    public void rejectCreate(Long metricId, String comment) {
        SysUser user = currentUserService.requireCurrentUser();
        MetricDefinition metric = metricRepository.findById(metricId)
                .orElseThrow(() -> new RuntimeException("指标不存在"));
        if (!"PENDING_APPROVAL".equals(metric.getStatus())) {
            throw new RuntimeException("该指标不在待审批状态");
        }
        topicApprovalService.assertCanApprove(user, metric.getTopicId());
        metric.setStatus("REJECTED");
        metric.setUpdatedBy(user.getUsername());
        metricRepository.save(metric);
    }

    @Transactional
    public MetricApprovalRequest approveUpdate(Long requestId, String comment) {
        SysUser user = currentUserService.requireCurrentUser();
        MetricApprovalRequest req = approvalRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("审批单不存在"));
        if (!STATUS_PENDING.equals(req.getStatus())) {
            throw new RuntimeException("审批单已处理");
        }
        if (!TYPE_UPDATE.equals(req.getRequestType())) {
            throw new RuntimeException("审批单类型不匹配");
        }
        topicApprovalService.assertCanApprove(user, req.getTopicId());
        req.setStatus("APPROVED");
        req.setReviewedBy(user.getUsername());
        req.setReviewComment(comment);
        req.setReviewedTime(LocalDateTime.now());
        approvalRepository.save(req);
        return req;
    }

    @Transactional
    public void rejectUpdate(Long requestId, String comment) {
        SysUser user = currentUserService.requireCurrentUser();
        MetricApprovalRequest req = approvalRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("审批单不存在"));
        if (!STATUS_PENDING.equals(req.getStatus())) {
            throw new RuntimeException("审批单已处理");
        }
        if (!TYPE_UPDATE.equals(req.getRequestType())) {
            throw new RuntimeException("审批单类型不匹配");
        }
        topicApprovalService.assertCanApprove(user, req.getTopicId());
        req.setStatus("REJECTED");
        req.setReviewedBy(user.getUsername());
        req.setReviewComment(comment);
        req.setReviewedTime(LocalDateTime.now());
        approvalRepository.save(req);
    }

    @Transactional
    public MetricApprovalRequest approveDeleteRequest(Long requestId, String comment) {
        SysUser user = currentUserService.requireCurrentUser();
        MetricApprovalRequest req = approvalRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("审批单不存在"));
        if (!STATUS_PENDING.equals(req.getStatus())) {
            throw new RuntimeException("审批单已处理");
        }
        if (!TYPE_DELETE.equals(req.getRequestType())) {
            throw new RuntimeException("审批单类型不匹配");
        }
        topicApprovalService.assertCanApprove(user, req.getTopicId());
        req.setStatus("APPROVED");
        req.setReviewedBy(user.getUsername());
        req.setReviewComment(comment);
        req.setReviewedTime(LocalDateTime.now());
        approvalRepository.save(req);
        return req;
    }

    @Transactional
    public void rejectDeleteRequest(Long requestId, String comment) {
        SysUser user = currentUserService.requireCurrentUser();
        MetricApprovalRequest req = approvalRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("审批单不存在"));
        if (!STATUS_PENDING.equals(req.getStatus())) {
            throw new RuntimeException("审批单已处理");
        }
        if (!TYPE_DELETE.equals(req.getRequestType())) {
            throw new RuntimeException("审批单类型不匹配");
        }
        topicApprovalService.assertCanApprove(user, req.getTopicId());
        req.setStatus("REJECTED");
        req.setReviewedBy(user.getUsername());
        req.setReviewComment(comment);
        req.setReviewedTime(LocalDateTime.now());
        approvalRepository.save(req);
    }

    public boolean hasPendingUpdate(Long metricId) {
        return approvalRepository.findByMetricIdAndStatus(metricId, STATUS_PENDING).stream()
                .anyMatch(r -> TYPE_UPDATE.equals(r.getRequestType()));
    }

    public boolean hasPendingDelete(Long metricId) {
        return approvalRepository.findByMetricIdAndStatus(metricId, STATUS_PENDING).stream()
                .anyMatch(r -> TYPE_DELETE.equals(r.getRequestType()));
    }

    public boolean isLockedForEdit(MetricDefinition metric) {
        if (metric == null) {
            return false;
        }
        if ("PENDING_APPROVAL".equals(metric.getStatus())) {
            return true;
        }
        if (metric.getId() == null) {
            return false;
        }
        return hasPendingUpdate(metric.getId()) || hasPendingDelete(metric.getId());
    }

    public java.util.Set<Long> findMetricIdsWithPendingUpdate(java.util.Collection<Long> metricIds) {
        if (metricIds == null || metricIds.isEmpty()) {
            return java.util.Set.of();
        }
        return approvalRepository
                .findByMetricIdInAndStatusAndRequestType(
                        new ArrayList<>(metricIds), STATUS_PENDING, TYPE_UPDATE)
                .stream()
                .map(MetricApprovalRequest::getMetricId)
                .collect(java.util.stream.Collectors.toSet());
    }

    public java.util.Set<Long> findMetricIdsWithPendingDelete(java.util.Collection<Long> metricIds) {
        if (metricIds == null || metricIds.isEmpty()) {
            return java.util.Set.of();
        }
        return approvalRepository
                .findByMetricIdInAndStatusAndRequestType(
                        new ArrayList<>(metricIds), STATUS_PENDING, TYPE_DELETE)
                .stream()
                .map(MetricApprovalRequest::getMetricId)
                .collect(java.util.stream.Collectors.toSet());
    }

    public List<SubmissionItemDto> listMySubmissions() {
        SysUser user = currentUserService.requireCurrentUser();
        String username = user.getUsername();
        List<SubmissionItemDto> items = new ArrayList<>();

        for (MetricDefinition metric : metricRepository.findByIsDeletedFalseOrderByCreatedTimeDesc()) {
            if (!username.equals(metric.getCreatedBy())) {
                continue;
            }
            if ("PENDING_APPROVAL".equals(metric.getStatus()) || "REJECTED".equals(metric.getStatus())) {
                SubmissionItemDto dto = new SubmissionItemDto();
                dto.setRequestType(TYPE_CREATE);
                dto.setMetricId(metric.getId());
                dto.setMetricName(metric.getMetricName());
                dto.setMetricCode(metric.getMetricCode());
                dto.setTopicName(metric.getTopicName());
                dto.setStatus(metric.getStatus());
                dto.setSubmittedTime(metric.getCreatedTime());
                items.add(dto);
            }
        }

        for (MetricApprovalRequest req : approvalRepository.findBySubmittedByOrderByCreatedTimeDesc(username)) {
            if (TYPE_UPDATE.equals(req.getRequestType())) {
                if (STATUS_PENDING.equals(req.getStatus()) || "REJECTED".equals(req.getStatus())) {
                    MetricDefinition existing = metricRepository.findById(req.getMetricId()).orElse(null);
                    if (existing == null) {
                        continue;
                    }
                    SubmissionItemDto dto = new SubmissionItemDto();
                    dto.setRequestType(TYPE_UPDATE);
                    dto.setRequestId(req.getId());
                    dto.setMetricId(req.getMetricId());
                    dto.setMetricName(existing.getMetricName());
                    dto.setMetricCode(existing.getMetricCode());
                    dto.setTopicName(existing.getTopicName());
                    dto.setStatus(req.getStatus());
                    dto.setReviewComment(req.getReviewComment());
                    dto.setSubmittedTime(req.getCreatedTime());
                    dto.setReviewedTime(req.getReviewedTime());
                    try {
                        MetricDefinition preview = objectMapper.readValue(req.getPayload(), MetricDefinition.class);
                        dto.setChangedFields(MetricDiffHelper.diffFields(existing, preview));
                    } catch (Exception ignored) {
                        // payload 解析失败时仍返回申请摘要
                    }
                    items.add(dto);
                }
            } else if (TYPE_DELETE.equals(req.getRequestType())
                    && (STATUS_PENDING.equals(req.getStatus()) || "REJECTED".equals(req.getStatus()))) {
                MetricDefinition existing = metricRepository.findById(req.getMetricId()).orElse(null);
                if (existing == null || Boolean.TRUE.equals(existing.getIsDeleted())) {
                    continue;
                }
                SubmissionItemDto dto = new SubmissionItemDto();
                dto.setRequestType(TYPE_DELETE);
                dto.setRequestId(req.getId());
                dto.setMetricId(req.getMetricId());
                dto.setMetricName(existing.getMetricName());
                dto.setMetricCode(existing.getMetricCode());
                dto.setTopicName(existing.getTopicName());
                dto.setStatus(req.getStatus());
                dto.setReviewComment(req.getReviewComment());
                dto.setSubmittedTime(req.getCreatedTime());
                dto.setReviewedTime(req.getReviewedTime());
                items.add(dto);
            }
        }

        items.sort(Comparator.comparing(SubmissionItemDto::getSubmittedTime,
                Comparator.nullsLast(Comparator.reverseOrder())));
        return items;
    }
}
