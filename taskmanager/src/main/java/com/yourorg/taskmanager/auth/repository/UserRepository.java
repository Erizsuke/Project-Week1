package com.yourorg.taskmanager.auth.repository;

import com.yourorg.taskmanager.auth.entity.User;
import com.yourorg.taskmanager.auth.entity.SystemRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.List;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    List<User> findAllByOrderByCreatedAtDesc();
    long countBySystemRoleAndActiveTrue(SystemRole systemRole);
}
