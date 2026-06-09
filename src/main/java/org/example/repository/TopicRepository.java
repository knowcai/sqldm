package org.example.repository;

import org.example.entity.Topic;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TopicRepository extends JpaRepository<Topic, Long> {
    
    Optional<Topic> findByTopicCodeAndIsDeletedFalse(String topicCode);
    
    List<Topic> findByIsDeletedFalseOrderByCreatedTimeDesc();
    
    Optional<Topic> findByTopicNameAndIsDeletedFalse(String topicName);
}
