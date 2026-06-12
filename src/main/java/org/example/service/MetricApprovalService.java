package org.example.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.example.dto.ApprovalItemDto;
import org.example.dto.SubmissionItemDto;
import org.example.entity.MetricApprovalRequest;
import org.example.entity.MetricDefinition;
import org.example.entity.SysUser;
import org.example.repository.MetricApprovalRequestRepository;
import org.example.repository.MetricRepository;
import org.example.util.MetricDiffHelper;
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

    private final MetricApprovalRequestRepository approvalRepository;
    private final MetricRepository metricRepository;
    private final TopicApprovalService topicApprovalService;
    private final CurrentUserService currentUserService;
    private final MetricTagService metricTagService;
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
        return updateCount + createCount;
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
            metric.setTags(metricTagService.getTagNames(metric.getId()));
            dto.setPreview(metric);
            items.add(dto);
        }

        for (MetricApprovalRequest req : approvalRepository
                .findByStatusAndTopicIdInOrderByCreatedTimeDesc(STATUS_PENDING, topicIds)) {
            if (!TYPE_UPDATE.equals(req.getRequestType())) {
                continue;
            }
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
            existing.setTags(metricTagService.getTagNames(req.getMetricId()));
            dto.setCurrent(existing);
            try {
                MetricDefinition preview = objectMapper.readValue(req.getPayload(), MetricDefinition.class);
                preview.setTags(metricTagService.getTagNames(req.getMetricId()));
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
                metric.setTags(metricTagService.getTagNames(metric.getId()));
                dto.setMetric(metric);
                items.add(dto);
            }
        }

        for (MetricApprovalRequest req : approvalRepository.findBySubmittedByOrderByCreatedTimeDesc(username)) {
            if (!TYPE_UPDATE.equals(req.getRequestType())) {
                continue;
            }
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
                items.add(dto);
            }
        }

        items.sort(Comparator.comparing(SubmissionItemDto::getSubmittedTime,
                Comparator.nullsLast(Comparator.reverseOrder())));
        return items;
    }
}
