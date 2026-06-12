package org.example.repository;

import org.example.entity.MetricTagRel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MetricTagRelRepository extends JpaRepository<MetricTagRel, Long> {

    List<MetricTagRel> findByMetricId(Long metricId);

    void deleteByMetricId(Long metricId);

    @Query("SELECT r.metricId FROM MetricTagRel r WHERE r.tagId = :tagId")
    List<Long> findMetricIdsByTagId(@Param("tagId") Long tagId);

    @Query("SELECT r.metricId FROM MetricTagRel r JOIN MetricTagRel r2 ON r.metricId = r2.metricId " +
           "WHERE r.tagId IN :tagIds GROUP BY r.metricId HAVING COUNT(DISTINCT r.tagId) = :tagCount")
    List<Long> findMetricIdsByAllTagIds(@Param("tagIds") List<Long> tagIds, @Param("tagCount") long tagCount);
}
