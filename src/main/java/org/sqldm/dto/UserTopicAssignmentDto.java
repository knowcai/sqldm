package org.sqldm.dto;

import lombok.Data;

@Data
public class UserTopicAssignmentDto {
    private Long topicId;
    private String topicName;
    private String role; // MEMBER, ADMIN
}
