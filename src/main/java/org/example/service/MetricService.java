package org.example.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.example.entity.MetricApprovalRequest;
import org.example.entity.MetricDefinition;
import org.example.entity.SysUser;
import org.example.entity.Topic;
import org.example.repository.MetricApprovalRequestRepository;
import org.example.repository.MetricRepository;
import org.example.repository.TopicRepository;
import org.example.openapi.util.OpenApiIpHelper;
import org.example.util.MetricCodeValidator;
import org.example.util.MetricDuplicateChecker;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MetricService {

    private static final Set<String> VALID_STAT_PERIODS = Set.of("DAY", "MONTH", "WEEK", "REALTIME");
    private static final Set<String> VALID_STATUSES = Set.of(
            "DRAFT", "PENDING_APPROVAL", "ACTIVE", "DISABLED", "REJECTED");

    private final MetricRepository metricRepository;
    private final TopicRepository topicRepository;
    private final MetricApprovalRequestRepository approvalRepository;
    private final MetricVersionAuditService versionAuditService;
    private final MetricTagService metricTagService;
    private final MetricEngagementService engagementService;
    private final WebhookNotificationService webhookNotificationService;
    private final MetricCodeValidator metricCodeValidator;
    private final MetricDuplicateChecker duplicateChecker;
    private final CurrentUserService currentUserService;
    private final TopicApprovalService topicApprovalService;
    private final MetricApprovalService metricApprovalService;
    private final ObjectMapper objectMapper;

    @Transactional
    public MetricDefinition createMetric(MetricDefinition metric) {
        SysUser user = currentUserService.requireCurrentUser();
        validateMetric(metric, null);
        metricCodeValidator.validate(metric.getMetricCode());

        if (metricRepository.findByMetricNameAndTopicIdAndIsDeletedFalse(metric.getMetricName(), metric.getTopicId())
                .isPresent()) {
            throw new RuntimeException("该主题下指标名称已存在: " + metric.getMetricName());
        }
        if (metricRepository.findByMetricCodeAndIsDeletedFalse(metric.getMetricCode()).isPresent()) {
            throw new RuntimeException("指标编码已存在: " + metric.getMetricCode());
        }

        fillTopicInfo(metric);
        boolean needsApproval = topicApprovalService.requiresApproval(user, metric.getTopicId());
        if (needsApproval) {
            metric.setStatus("PENDING_APPROVAL");
        } else if (!StringUtils.hasText(metric.getStatus())) {
            metric.setStatus("ACTIVE");
        }

        metric.setCreatedBy(user.getUsername());
        metric.setUpdatedBy(user.getUsername());
        metric.setIsDeleted(false);

        List<String> tagNames = metric.getTags();
        MetricDefinition saved = metricRepository.save(metric);
        metricTagService.setMetricTags(saved.getId(), tagNames);

        if (needsApproval) {
            versionAuditService.logAction(saved.getId(), "SUBMIT", "提交新增审批");
            webhookNotificationService.notify("METRIC_SUBMIT", saved, "提交新增审批");
        } else {
            versionAuditService.logAction(saved.getId(), "CREATE", "创建指标: " + saved.getMetricName());
            versionAuditService.snapshotIfActive(saved, "创建并启用");
            webhookNotificationService.notify("METRIC_CREATED", saved, "创建指标");
        }
        return enrich(saved);
    }

    @Transactional
    public MetricDefinition updateMetric(Long id, MetricDefinition metric) {
        SysUser user = currentUserService.requireCurrentUser();
        MetricDefinition existing = metricRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("指标不存在: " + id));

        validateMetric(metric, id);
        metricCodeValidator.validate(metric.getMetricCode());

        Long topicId = metric.getTopicId() != null ? metric.getTopicId() : existing.getTopicId();
        metric.setTopicId(topicId);

        if (!Objects.equals(existing.getMetricName(), metric.getMetricName())
                && metricRepository.findByMetricNameAndTopicIdAndIsDeletedFalse(metric.getMetricName(), topicId)
                        .isPresent()) {
            throw new RuntimeException("该主题下指标名称已存在: " + metric.getMetricName());
        }
        if (!Objects.equals(existing.getMetricCode(), metric.getMetricCode())
                && metricRepository.findByMetricCodeAndIsDeletedFalse(metric.getMetricCode()).isPresent()) {
            throw new RuntimeException("指标编码已存在: " + metric.getMetricCode());
        }

        if ("REJECTED".equals(existing.getStatus()) && topicApprovalService.requiresApproval(user, topicId)) {
            return resubmitRejectedCreate(existing, metric, user);
        }
        if (topicApprovalService.requiresApproval(user, topicId)) {
            return submitUpdateApproval(existing, metric, user);
        }
        return applyUpdateDirect(existing, metric);
    }

    private MetricDefinition resubmitRejectedCreate(MetricDefinition existing, MetricDefinition metric, SysUser user) {
        fillTopicInfo(metric);
        existing.setMetricName(metric.getMetricName());
        existing.setMetricCode(metric.getMetricCode());
        existing.setBusinessCaliber(metric.getBusinessCaliber());
        existing.setStatPeriod(metric.getStatPeriod());
        existing.setTopicId(metric.getTopicId());
        existing.setOwner(metric.getOwner());
        existing.setDataSource(metric.getDataSource());
        existing.setSqlTemplate(metric.getSqlTemplate());
        existing.setParamDefinition(metric.getParamDefinition());
        existing.setAllowedIps(metric.getAllowedIps());
        existing.setStatus("PENDING_APPROVAL");
        fillTopicInfo(existing);
        existing.setUpdatedBy(user.getUsername());
        MetricDefinition saved = metricRepository.save(existing);
        metricTagService.setMetricTags(saved.getId(), metric.getTags());
        versionAuditService.logAction(saved.getId(), "SUBMIT", "驳回后重新提交");
        webhookNotificationService.notify("METRIC_SUBMIT", saved, "驳回后重新提交");
        MetricDefinition result = enrich(saved);
        result.setDuplicateWarnings(metric.getDuplicateWarnings());
        return result;
    }

    @Transactional
    public MetricDefinition approveCreate(Long metricId, String comment) {
        metricApprovalService.approveCreate(metricId, comment);
        MetricDefinition saved = metricRepository.findById(metricId).orElseThrow();
        versionAuditService.logAction(metricId, "APPROVE", "审批通过新增: " + (comment != null ? comment : ""));
        versionAuditService.snapshotIfActive(saved, "审批通过并启用");
        webhookNotificationService.notify("METRIC_APPROVED", saved, "审批通过新增");
        webhookNotificationService.notify("METRIC_CREATED", saved, "审批通过新增");
        return enrich(saved);
    }

    @Transactional
    public void rejectCreate(Long metricId, String comment) {
        metricApprovalService.rejectCreate(metricId, comment);
        MetricDefinition saved = metricRepository.findById(metricId).orElseThrow();
        versionAuditService.logAction(metricId, "REJECT", "驳回新增: " + (comment != null ? comment : ""));
        webhookNotificationService.notify("METRIC_REJECTED", saved, "驳回新增: " + (comment != null ? comment : ""));
    }

    @Transactional
    public MetricDefinition approveUpdate(Long requestId, String comment) {
        MetricApprovalRequest req = metricApprovalService.approveUpdate(requestId, comment);
        MetricDefinition existing = metricRepository.findById(req.getMetricId())
                .orElseThrow(() -> new RuntimeException("指标不存在"));
        try {
            MetricDefinition payload = objectMapper.readValue(req.getPayload(), MetricDefinition.class);
            MetricDefinition saved = applyUpdateDirect(existing, payload);
            versionAuditService.logAction(saved.getId(), "APPROVE", "审批通过变更: " + (comment != null ? comment : ""));
            webhookNotificationService.notify("METRIC_APPROVED", saved, "审批通过变更");
            return enrich(saved);
        } catch (Exception e) {
            throw new RuntimeException("审批应用失败: " + e.getMessage());
        }
    }

    @Transactional
    public void rejectUpdate(Long requestId, String comment) {
        metricApprovalService.rejectUpdate(requestId, comment);
        MetricApprovalRequest req = approvalRepository.findById(requestId).orElseThrow();
        MetricDefinition metric = metricRepository.findById(req.getMetricId()).orElseThrow();
        versionAuditService.logAction(req.getMetricId(), "REJECT", "驳回变更: " + (comment != null ? comment : ""));
        webhookNotificationService.notify("METRIC_REJECTED", metric, "驳回变更: " + (comment != null ? comment : ""));
    }

    private MetricDefinition submitUpdateApproval(MetricDefinition existing, MetricDefinition metric, SysUser user) {
        try {
            fillTopicInfo(metric);
            metric.setId(existing.getId());
            String payload = objectMapper.writeValueAsString(metric);

            Optional<MetricApprovalRequest> pending = approvalRepository
                    .findByMetricIdAndStatusAndRequestType(existing.getId(),
                            MetricApprovalService.STATUS_PENDING, MetricApprovalService.TYPE_UPDATE);
            MetricApprovalRequest req = pending.orElseGet(MetricApprovalRequest::new);
            req.setMetricId(existing.getId());
            req.setTopicId(existing.getTopicId());
            req.setRequestType(MetricApprovalService.TYPE_UPDATE);
            req.setPayload(payload);
            req.setStatus(MetricApprovalService.STATUS_PENDING);
            req.setSubmittedBy(user.getUsername());
            approvalRepository.save(req);

            versionAuditService.logAction(existing.getId(), "SUBMIT", "提交变更审批");
            webhookNotificationService.notify("METRIC_SUBMIT", existing, "提交变更审批");
            MetricDefinition result = enrich(existing);
            result.setDuplicateWarnings(metric.getDuplicateWarnings());
            return result;
        } catch (Exception e) {
            throw new RuntimeException("提交审批失败: " + e.getMessage());
        }
    }

    private MetricDefinition applyUpdateDirect(MetricDefinition existing, MetricDefinition metric) {
        String oldStatus = existing.getStatus();
        existing.setMetricName(metric.getMetricName());
        existing.setMetricCode(metric.getMetricCode());
        existing.setBusinessCaliber(metric.getBusinessCaliber());
        existing.setStatPeriod(metric.getStatPeriod());
        existing.setTopicId(metric.getTopicId());
        existing.setOwner(metric.getOwner());
        existing.setStatus(metric.getStatus());
        existing.setDataSource(metric.getDataSource());
        existing.setSqlTemplate(metric.getSqlTemplate());
        existing.setParamDefinition(metric.getParamDefinition());
        existing.setAllowedIps(metric.getAllowedIps());
        fillTopicInfo(existing);
        existing.setUpdatedBy(currentUserService.getCurrentUsername());

        MetricDefinition saved = metricRepository.save(existing);
        metricTagService.setMetricTags(saved.getId(), metric.getTags());
        versionAuditService.logAction(saved.getId(), "UPDATE", "更新指标配置");
        if (!Objects.equals(oldStatus, saved.getStatus())) {
            versionAuditService.logAction(saved.getId(), "STATUS_CHANGE",
                    oldStatus + " -> " + saved.getStatus());
            webhookNotificationService.notify("METRIC_STATUS_CHANGE", saved, oldStatus + " -> " + saved.getStatus());
        }
        versionAuditService.snapshotIfActive(saved, "更新指标");
        webhookNotificationService.notify("METRIC_UPDATED", saved, "更新指标配置");
        return enrich(saved);
    }

    @Transactional
    public void deleteMetric(Long id) {
        SysUser user = currentUserService.requireCurrentUser();
        MetricDefinition metric = metricRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("指标不存在: " + id));

        if (!topicApprovalService.canDeleteMetric(user, metric.getTopicId())) {
            throw new RuntimeException("无权删除该指标，请联系主题审批员或超级管理员");
        }

        metric.setIsDeleted(true);
        metric.setUpdatedBy(currentUserService.getCurrentUsername());
        metricRepository.save(metric);
        versionAuditService.logAction(id, "DELETE", "删除指标: " + metric.getMetricName());
        webhookNotificationService.notify("METRIC_DELETED", metric, "删除指标");
    }

    public List<MetricDefinition> listMetrics(String keyword, Long topicId, String status, String tag, String sort) {
        return queryMetrics(keyword, topicId, status, tag, sort);
    }

    public List<MetricDefinition> queryMetrics(String keyword, Long topicId, String status, String tag, String sort) {
        List<MetricDefinition> metrics;
        if (StringUtils.hasText(keyword)) {
            metrics = new ArrayList<>(metricRepository.searchMetrics(keyword.trim()));
        } else {
            metrics = new ArrayList<>(metricRepository.findByIsDeletedFalseOrderByCreatedTimeDesc());
        }
        if (topicId != null) {
            metrics = metrics.stream()
                    .filter(m -> topicId.equals(m.getTopicId()))
                    .collect(Collectors.toList());
        }
        if (StringUtils.hasText(status)) {
            String s = status.trim();
            metrics = metrics.stream().filter(m -> s.equals(m.getStatus())).collect(Collectors.toList());
        }
        if (StringUtils.hasText(tag)) {
            List<Long> tagIds = metricTagService.findMetricIdsByTagName(tag.trim());
            Set<Long> idSet = new HashSet<>(tagIds);
            metrics = metrics.stream().filter(m -> idSet.contains(m.getId())).collect(Collectors.toList());
        }
        sortMetrics(metrics, sort);
        return enrichAll(metrics);
    }

    public List<MetricDefinition> getAllMetrics() {
        return enrichAll(metricRepository.findByIsDeletedFalseOrderByCreatedTimeDesc());
    }

    public MetricDefinition getMetricById(Long id) {
        MetricDefinition metric = metricRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("指标不存在: " + id));
        engagementService.recordRecentAccess(id);
        return enrich(metric);
    }

    public List<MetricDefinition> searchMetrics(String keyword) {
        return queryMetrics(keyword, null, null, null, null);
    }

    public List<MetricDefinition> getMetricsByTopic(Long topicId) {
        return enrichAll(metricRepository.findByTopicIdAndIsDeletedFalseOrderByCreatedTimeDesc(topicId));
    }

    public List<MetricDefinition> getMetricsByTag(String tagName) {
        List<Long> ids = metricTagService.findMetricIdsByTagName(tagName);
        if (ids.isEmpty()) {
            return List.of();
        }
        return enrichAll(metricRepository.findByIdInAndIsDeletedFalse(ids));
    }

    public List<String> checkDuplicateWarnings(MetricDefinition metric, Long excludeId) {
        return duplicateChecker.findWarnings(metric, excludeId);
    }

    private void sortMetrics(List<MetricDefinition> metrics, String sort) {
        if (sort == null || sort.isBlank()) {
            return;
        }
        String[] parts = sort.split(",");
        String field = parts[0];
        boolean asc = parts.length > 1 && "asc".equalsIgnoreCase(parts[1]);
        Comparator<MetricDefinition> cmp;
        if ("metricName".equals(field)) {
            cmp = Comparator.comparing(MetricDefinition::getMetricName,
                    Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
        } else if ("updatedTime".equals(field)) {
            cmp = Comparator.comparing(MetricDefinition::getUpdatedTime,
                    Comparator.nullsLast(Comparator.naturalOrder()));
        } else {
            cmp = Comparator.comparing(MetricDefinition::getCreatedTime,
                    Comparator.nullsLast(Comparator.naturalOrder()));
        }
        if (!asc) {
            cmp = cmp.reversed();
        }
        metrics.sort(cmp);
    }

    private MetricDefinition enrich(MetricDefinition metric) {
        if (metric == null || metric.getId() == null) {
            return metric;
        }
        metric.setTags(metricTagService.getTagNames(metric.getId()));
        try {
            metric.setFavorited(engagementService.isFavorite(metric.getId()));
        } catch (Exception ignored) {
            metric.setFavorited(false);
        }
        metric.setHasPendingUpdate(metricApprovalService.hasPendingUpdate(metric.getId()));
        return metric;
    }

    private List<MetricDefinition> enrichAll(List<MetricDefinition> metrics) {
        if (metrics.isEmpty()) {
            return metrics;
        }
        List<Long> ids = metrics.stream().map(MetricDefinition::getId).collect(Collectors.toList());
        Map<Long, List<String>> tagMap = metricTagService.getTagNamesBatch(ids);
        Set<Long> pendingUpdateIds = metricApprovalService.findMetricIdsWithPendingUpdate(ids);
        Map<Long, Boolean> favoriteMap = engagementService.isFavoriteBatch(ids);
        for (MetricDefinition m : metrics) {
            m.setTags(tagMap.getOrDefault(m.getId(), List.of()));
            m.setHasPendingUpdate(pendingUpdateIds.contains(m.getId()));
            m.setFavorited(favoriteMap.getOrDefault(m.getId(), false));
        }
        return metrics;
    }

    private void validateMetric(MetricDefinition metric, Long excludeId) {
        if (!StringUtils.hasText(metric.getMetricName())) {
            throw new RuntimeException("指标名称不能为空");
        }
        if (!StringUtils.hasText(metric.getMetricCode())) {
            throw new RuntimeException("指标编码不能为空");
        }
        if (!StringUtils.hasText(metric.getBusinessCaliber())) {
            throw new RuntimeException("业务口径不能为空");
        }
        if (metric.getTopicId() == null) {
            throw new RuntimeException("所属主题域不能为空");
        }
        if (!StringUtils.hasText(metric.getOwner())) {
            throw new RuntimeException("负责人不能为空");
        }
        String st = metric.getStatus();
        if (!"DRAFT".equals(st) && !"PENDING_APPROVAL".equals(st)) {
            if (!StringUtils.hasText(metric.getDataSource())) {
                throw new RuntimeException("数据源不能为空");
            }
            if (!StringUtils.hasText(metric.getSqlTemplate())) {
                throw new RuntimeException("SQL模版不能为空");
            }
        }
        if (StringUtils.hasText(metric.getParamDefinition())) {
            try {
                objectMapper.readTree(metric.getParamDefinition());
            } catch (Exception e) {
                throw new RuntimeException("参数定义必须是合法的 JSON 格式");
            }
        }
        OpenApiIpHelper.validateAllowedIpsJson(metric.getAllowedIps());
        if (!StringUtils.hasText(metric.getStatus())) {
            throw new RuntimeException("状态不能为空");
        }
        if (!VALID_STATUSES.contains(metric.getStatus())) {
            throw new RuntimeException("无效的状态: " + metric.getStatus());
        }
        if (StringUtils.hasText(metric.getStatPeriod()) && !VALID_STAT_PERIODS.contains(metric.getStatPeriod())) {
            throw new RuntimeException("无效的统计周期: " + metric.getStatPeriod());
        }
        List<String> warnings = duplicateChecker.findWarnings(metric, excludeId);
        if (!warnings.isEmpty()) {
            metric.setDuplicateWarnings(warnings);
        }
    }

    private void fillTopicInfo(MetricDefinition metric) {
        Topic topic = topicRepository.findById(metric.getTopicId())
                .orElseThrow(() -> new RuntimeException("主题不存在: " + metric.getTopicId()));
        metric.setTopicName(topic.getTopicName());
    }
}
