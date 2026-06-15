package org.sqldm.repository;

import org.sqldm.entity.MetricDefinition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MetricRepository extends JpaRepository<MetricDefinition, Long> {

    Optional<MetricDefinition> findByMetricNameAndIsDeletedFalse(String metricName);

    Optional<MetricDefinition> findByMetricNameAndTopicIdAndIsDeletedFalse(String metricName, Long topicId);

    Optional<MetricDefinition> findByMetricCodeAndIsDeletedFalse(String metricCode);

    List<MetricDefinition> findByIsDeletedFalseOrderByCreatedTimeDesc();

    @Query("SELECT m FROM MetricDefinition m WHERE m.isDeleted = false AND " +
           "(LOWER(m.metricName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(m.metricCode) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(m.businessCaliber) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(m.owner) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(m.dataSource) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    List<MetricDefinition> searchMetrics(@Param("keyword") String keyword);

    List<MetricDefinition> findByTopicIdAndIsDeletedFalseOrderByCreatedTimeDesc(Long topicId);

    List<MetricDefinition> findByStatusAndIsDeletedFalseOrderByCreatedTimeDesc(String status);

    Optional<MetricDefinition> findByMetricCodeAndStatusAndIsDeletedFalse(String metricCode, String status);

    Optional<MetricDefinition> findByIdAndStatusAndIsDeletedFalse(Long id, String status);

    List<MetricDefinition> findByOwnerAndIsDeletedFalseOrderByCreatedTimeDesc(String owner);

    List<MetricDefinition> findByTopicIdInAndIsDeletedFalseOrderByCreatedTimeDesc(List<Long> topicIds);

    List<MetricDefinition> findByIdInAndIsDeletedFalse(List<Long> ids);

    List<MetricDefinition> findByStatusAndTopicIdInAndIsDeletedFalseOrderByCreatedTimeDesc(
            String status, List<Long> topicIds);
}
