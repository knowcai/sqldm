package org.example.repository;

import org.example.entity.MetricTag;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MetricTagRepository extends JpaRepository<MetricTag, Long> {

    Optional<MetricTag> findByTagNameIgnoreCase(String tagName);

    List<MetricTag> findAllByOrderByTagNameAsc();
}
