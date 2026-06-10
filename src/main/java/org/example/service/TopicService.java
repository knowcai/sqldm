package org.example.service;

import lombok.RequiredArgsConstructor;
import org.example.entity.Topic;
import org.example.repository.TopicRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TopicService {

    private final TopicRepository topicRepository;

    /**
     * 创建主题
     */
    @Transactional
    public Topic createTopic(Topic topic) {
        if (topicRepository.findByTopicNameAndIsDeletedFalse(topic.getTopicName()).isPresent()) {
            throw new RuntimeException("主题名称已存在: " + topic.getTopicName());
        }

        topic.setTopicCode(generateTopicCode());
        topic.setIsDeleted(false);
        return topicRepository.save(topic);
    }

    /**
     * 更新主题
     */
    @Transactional
    public Topic updateTopic(Long id, Topic topic) {
        Topic existing = topicRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("主题不存在: " + id));

        if (!existing.getTopicName().equals(topic.getTopicName())) {
            if (topicRepository.findByTopicNameAndIsDeletedFalse(topic.getTopicName()).isPresent()) {
                throw new RuntimeException("主题名称已存在: " + topic.getTopicName());
            }
        }

        existing.setTopicName(topic.getTopicName());
        existing.setDescription(topic.getDescription());
        existing.setAdminId(topic.getAdminId());

        return topicRepository.save(existing);
    }

    /**
     * 删除主题（逻辑删除）
     */
    @Transactional
    public void deleteTopic(Long id) {
        Topic topic = topicRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("主题不存在: " + id));
        
        topic.setIsDeleted(true);
        topicRepository.save(topic);
    }

    /**
     * 获取所有未删除的主题
     */
    public List<Topic> getAllTopics() {
        return topicRepository.findByIsDeletedFalseOrderByCreatedTimeDesc();
    }

    /**
     * 根据ID获取主题
     */
    public Topic getTopicById(Long id) {
        return topicRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("主题不存在: " + id));
    }

    private String generateTopicCode() {
        return "topic_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }
}
