package org.sqldm.dto;

import lombok.Data;

import java.util.List;

@Data
public class UserSaveRequest {
    private String username;
    private String password;
    private String realName;
    private String email;
    private String role;
    private Boolean isActive;
    private List<UserTopicAssignmentDto> topicAssignments;
}
