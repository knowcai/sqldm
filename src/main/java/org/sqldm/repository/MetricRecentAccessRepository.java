package org.sqldm.repository;

import org.sqldm.entity.MetricRecentAccess;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MetricRecentAccessRepository extends JpaRepository<MetricRecentAccess, Long> {

    List<MetricRecentAccess> findTop20ByUserIdOrderByAccessedTimeDesc(Long userId);

    Optional<MetricRecentAccess> findByUserIdAndMetricId(Long userId, Long metricId);
}
