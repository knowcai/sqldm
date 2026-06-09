package org.example.repository;

import org.example.entity.SysUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SysUserRepository extends JpaRepository<SysUser, Long> {
    
    Optional<SysUser> findByUsernameAndIsDeletedFalse(String username);
    
    List<SysUser> findByIsDeletedFalseOrderByCreatedTimeDesc();
    
    List<SysUser> findByRoleAndIsDeletedFalse(String role);
    
    List<SysUser> findByIsActiveTrueAndIsDeletedFalse();
}
