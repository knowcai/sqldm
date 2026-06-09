package org.example.service;

import lombok.RequiredArgsConstructor;
import org.example.entity.Topic;
import org.example.repository.TopicRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TopicService {

    private final TopicRepository topicRepository;

    /**
     * 创建主题
     */
    @Transactional
    public Topic createTopic(Topic topic) {
        // 检查主题代码是否已存在
        if (topicRepository.findByTopicCodeAndIsDeletedFalse(topic.getTopicCode()).isPresent()) {
            throw new RuntimeException("主题代码已存在: " + topic.getTopicCode());
        }
        
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

        // 检查新代码是否与其他主题冲突
        if (!existing.getTopicCode().equals(topic.getTopicCode())) {
            if (topicRepository.findByTopicCodeAndIsDeletedFalse(topic.getTopicCode()).isPresent()) {
                throw new RuntimeException("主题代码已存在: " + topic.getTopicCode());
            }
        }

        existing.setTopicName(topic.getTopicName());
        existing.setTopicCode(topic.getTopicCode());
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
}
