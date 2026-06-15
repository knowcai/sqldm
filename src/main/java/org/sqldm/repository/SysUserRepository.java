package org.sqldm.repository;

import org.sqldm.entity.SysUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SysUserRepository extends JpaRepository<SysUser, Long> {

    Optional<SysUser> findByUsername(String username);

    List<SysUser> findAllByOrderByCreatedTimeDesc();

    List<SysUser> findByRole(String role);

    List<SysUser> findByIsActiveTrue();
}
