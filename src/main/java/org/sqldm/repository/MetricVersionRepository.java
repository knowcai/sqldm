package org.sqldm.repository;

import org.sqldm.entity.MetricVersion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MetricVersionRepository extends JpaRepository<MetricVersion, Long> {

    List<MetricVersion> findByMetricIdOrderByVersionNoDesc(Long metricId);

    Optional<MetricVersion> findTopByMetricIdOrderByVersionNoDesc(Long metricId);

    Optional<MetricVersion> findByMetricIdAndVersionNo(Long metricId, Integer versionNo);
}
