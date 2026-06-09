package org.example.repository;

import org.example.entity.UserTopic;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserTopicRepository extends JpaRepository<UserTopic, Long> {
    
    List<UserTopic> findByUserId(Long userId);
    
    List<UserTopic> findByTopicId(Long topicId);
    
    Optional<UserTopic> findByUserIdAndTopicId(Long userId, Long topicId);
    
    List<UserTopic> findByUserIdAndRole(Long userId, String role);
    
    List<UserTopic> findByTopicIdAndRole(Long topicId, String role);
}
