package org.example.repository;

import org.example.entity.MetricFavorite;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MetricFavoriteRepository extends JpaRepository<MetricFavorite, Long> {

    List<MetricFavorite> findByUserIdOrderByCreatedTimeDesc(Long userId);

    Optional<MetricFavorite> findByUserIdAndMetricId(Long userId, Long metricId);

    void deleteByUserIdAndMetricId(Long userId, Long metricId);

    boolean existsByUserIdAndMetricId(Long userId, Long metricId);
}
