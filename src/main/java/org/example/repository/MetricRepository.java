package org.example.repository;

import org.example.entity.MetricDefinition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MetricRepository extends JpaRepository<MetricDefinition, Long> {

    Optional<MetricDefinition> findByMetricNameAndIsDeletedFalse(String metricName);

    List<MetricDefinition> findByIsDeletedFalseOrderByCreatedTimeDesc();

    List<MetricDefinition> findByMetricTypeAndIsDeletedFalse(String metricType);

    @Query("SELECT m FROM MetricDefinition m WHERE m.isDeleted = false AND " +
           "(LOWER(m.metricName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(m.description) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    List<MetricDefinition> searchMetrics(@Param("keyword") String keyword);
    
    List<MetricDefinition> findByTopicIdAndIsDeletedFalse(Long topicId);
    
    List<MetricDefinition> findByTopicIdAndIsDeletedFalseOrderByCreatedTimeDesc(Long topicId);
}
