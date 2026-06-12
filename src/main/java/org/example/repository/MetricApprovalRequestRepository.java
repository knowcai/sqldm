package org.example.repository;

import org.example.entity.MetricApprovalRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MetricApprovalRequestRepository extends JpaRepository<MetricApprovalRequest, Long> {

    List<MetricApprovalRequest> findByStatusAndTopicIdInOrderByCreatedTimeDesc(String status, List<Long> topicIds);

    Optional<MetricApprovalRequest> findByMetricIdAndStatusAndRequestType(Long metricId, String status, String requestType);

    List<MetricApprovalRequest> findByMetricIdAndStatus(Long metricId, String status);

    long countByStatusAndTopicIdIn(String status, List<Long> topicIds);

    List<MetricApprovalRequest> findBySubmittedByOrderByCreatedTimeDesc(String submittedBy);

    List<MetricApprovalRequest> findByMetricIdInAndStatusAndRequestType(
            List<Long> metricIds, String status, String requestType);
}
