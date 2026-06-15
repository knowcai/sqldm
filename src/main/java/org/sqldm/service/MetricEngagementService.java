package org.sqldm.service;

import lombok.RequiredArgsConstructor;
import org.sqldm.entity.MetricDefinition;
import org.sqldm.entity.MetricRecentAccess;
import org.sqldm.entity.SysUser;
import org.sqldm.entity.UserTopic;
import org.sqldm.repository.MetricRecentAccessRepository;
import org.sqldm.repository.MetricRepository;
import org.sqldm.repository.UserTopicRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MetricEngagementService {

    private final MetricRecentAccessRepository recentRepository;
    private final MetricRepository metricRepository;
    private final UserTopicRepository userTopicRepository;
    private final CurrentUserService currentUserService;

    @Transactional
    public void recordRecentAccess(Long metricId) {
        SysUser user = currentUserService.requireCurrentUser();
        MetricRecentAccess access = recentRepository.findByUserIdAndMetricId(user.getId(), metricId)
                .orElseGet(() -> {
                    MetricRecentAccess r = new MetricRecentAccess();
                    r.setUserId(user.getId());
                    r.setMetricId(metricId);
                    return r;
                });
        recentRepository.save(access);
    }

    public List<MetricDefinition> listRecent() {
        SysUser user = currentUserService.requireCurrentUser();
        List<Long> metricIds = recentRepository.findTop20ByUserIdOrderByAccessedTimeDesc(user.getId())
                .stream().map(MetricRecentAccess::getMetricId).toList();
        return loadMetricsInOrder(metricIds);
    }

    public List<MetricDefinition> listMine() {
        SysUser user = currentUserService.requireCurrentUser();
        List<MetricDefinition> result = new ArrayList<>();
        result.addAll(metricRepository.findByOwnerAndIsDeletedFalseOrderByCreatedTimeDesc(user.getUsername()));
        if (StringUtils.hasText(user.getRealName())) {
            metricRepository.findByOwnerAndIsDeletedFalseOrderByCreatedTimeDesc(user.getRealName())
                    .forEach(m -> {
                        if (result.stream().noneMatch(x -> Objects.equals(x.getId(), m.getId()))) {
                            result.add(m);
                        }
                    });
        }
        return result;
    }

    public List<MetricDefinition> listMyTopicMetrics() {
        SysUser user = currentUserService.requireCurrentUser();
        Set<Long> topicIds = userTopicRepository.findByUserId(user.getId()).stream()
                .map(UserTopic::getTopicId)
                .collect(Collectors.toSet());
        if (topicIds.isEmpty()) {
            return List.of();
        }
        return metricRepository.findByTopicIdInAndIsDeletedFalseOrderByCreatedTimeDesc(new ArrayList<>(topicIds));
    }

    private List<MetricDefinition> loadMetricsInOrder(List<Long> metricIds) {
        if (metricIds.isEmpty()) {
            return List.of();
        }
        List<MetricDefinition> all = metricRepository.findByIdInAndIsDeletedFalse(metricIds);
        return metricIds.stream()
                .map(id -> all.stream().filter(m -> Objects.equals(m.getId(), id)).findFirst().orElse(null))
                .filter(Objects::nonNull)
                .toList();
    }
}
