package org.example.service;

import lombok.RequiredArgsConstructor;
import org.example.entity.SysUser;
import org.example.repository.SysUserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CurrentUserService {

    private final SysUserRepository userRepository;

    public SysUser requireCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new RuntimeException("未登录");
        }
        return userRepository.findByUsernameAndIsDeletedFalse(auth.getName())
                .orElseThrow(() -> new RuntimeException("用户不存在"));
    }

    public String getCurrentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()) {
            return auth.getName();
        }
        return "system";
    }
}
