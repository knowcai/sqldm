package org.example.repository;

import org.example.entity.ApiAccessLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ApiAccessLogRepository extends JpaRepository<ApiAccessLog, Long> {

    List<ApiAccessLog> findTop200ByOrderByCreatedTimeDesc();

    long countBySuccessTrue();

    @Query("SELECT l.metricCode, COUNT(l) FROM ApiAccessLog l GROUP BY l.metricCode ORDER BY COUNT(l) DESC")
    List<Object[]> topMetricsByCallCount();

    @Query(value = "SELECT l.api_key_hint, COUNT(*) FROM api_access_log l GROUP BY l.api_key_hint ORDER BY COUNT(*) DESC LIMIT 10",
            nativeQuery = true)
    List<Object[]> topApiKeysByCallCount();
}
