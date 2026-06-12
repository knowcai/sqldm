package org.example.service;

import lombok.RequiredArgsConstructor;
import org.example.entity.MetricTag;
import org.example.entity.MetricTagRel;
import org.example.repository.MetricTagRelRepository;
import org.example.repository.MetricTagRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MetricTagService {

    private final MetricTagRepository tagRepository;
    private final MetricTagRelRepository relRepository;

    public List<MetricTag> listAllTags() {
        return tagRepository.findAllByOrderByTagNameAsc();
    }

    @Transactional
    public MetricTag createTag(String tagName) {
        if (!StringUtils.hasText(tagName)) {
            throw new RuntimeException("标签名不能为空");
        }
        String normalized = tagName.trim();
        return tagRepository.findByTagNameIgnoreCase(normalized)
                .orElseGet(() -> {
                    MetricTag tag = new MetricTag();
                    tag.setTagName(normalized);
                    return tagRepository.save(tag);
                });
    }

    @Transactional
    public void setMetricTags(Long metricId, List<String> tagNames) {
        relRepository.deleteByMetricId(metricId);
        if (tagNames == null || tagNames.isEmpty()) {
            return;
        }
        for (String name : tagNames) {
            if (!StringUtils.hasText(name)) {
                continue;
            }
            MetricTag tag = createTag(name.trim());
            MetricTagRel rel = new MetricTagRel();
            rel.setMetricId(metricId);
            rel.setTagId(tag.getId());
            relRepository.save(rel);
        }
    }

    public List<String> getTagNames(Long metricId) {
        List<MetricTagRel> rels = relRepository.findByMetricId(metricId);
        if (rels.isEmpty()) {
            return List.of();
        }
        Set<Long> tagIds = rels.stream().map(MetricTagRel::getTagId).collect(Collectors.toSet());
        return tagRepository.findAllById(tagIds).stream()
                .map(MetricTag::getTagName)
                .sorted()
                .toList();
    }

    public Map<Long, List<String>> getTagNamesBatch(Collection<Long> metricIds) {
        Map<Long, List<String>> result = new HashMap<>();
        if (metricIds == null || metricIds.isEmpty()) {
            return result;
        }
        Map<Long, MetricTag> tagMap = tagRepository.findAll().stream()
                .collect(Collectors.toMap(MetricTag::getId, t -> t));
        for (Long metricId : metricIds) {
            List<String> names = relRepository.findByMetricId(metricId).stream()
                    .map(rel -> tagMap.get(rel.getTagId()))
                    .filter(Objects::nonNull)
                    .map(MetricTag::getTagName)
                    .sorted()
                    .toList();
            result.put(metricId, names);
        }
        return result;
    }

    public List<Long> findMetricIdsByTagName(String tagName) {
        if (!StringUtils.hasText(tagName)) {
            return List.of();
        }
        return tagRepository.findByTagNameIgnoreCase(tagName.trim())
                .map(tag -> relRepository.findMetricIdsByTagId(tag.getId()))
                .orElse(List.of());
    }
}
