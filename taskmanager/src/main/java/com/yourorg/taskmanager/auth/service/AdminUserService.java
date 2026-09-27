package com.yourorg.taskmanager.auth.service;

import com.yourorg.taskmanager.auth.dto.AdminUpdateUserRequest;
import com.yourorg.taskmanager.auth.dto.AdminUserResponse;
import com.yourorg.taskmanager.auth.entity.SystemRole;
import com.yourorg.taskmanager.auth.entity.User;
import com.yourorg.taskmanager.auth.repository.UserRepository;
import com.yourorg.taskmanager.common.exception.AccessDeniedCustomException;
import com.yourorg.taskmanager.common.exception.BadRequestException;
import com.yourorg.taskmanager.common.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class AdminUserService {

    private final UserRepository userRepository;

    public AdminUserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<AdminUserResponse> list(User actor) {
        requireAdmin(actor);
        return userRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(AdminUserResponse::from).toList();
    }

    @Transactional
    public AdminUserResponse update(User actor, UUID userId, AdminUpdateUserRequest request) {
        requireAdmin(actor);
        User user = findUser(userId);
        if (request.systemRole() != null && request.systemRole() != user.getSystemRole()) {
            protectLastAdmin(user);
            user.setSystemRole(request.systemRole());
        }
        return AdminUserResponse.from(userRepository.save(user));
    }

    @Transactional
    public void deactivate(User actor, UUID userId) {
        requireAdmin(actor);
        if (actor.getId().equals(userId)) {
            throw new BadRequestException("You cannot deactivate your own account");
        }
        User user = findUser(userId);
        protectLastAdmin(user);
        user.setActive(false);
        userRepository.save(user);
    }

    private void protectLastAdmin(User user) {
        if (user.isActive() && user.getSystemRole() == SystemRole.ADMIN
                && userRepository.countBySystemRoleAndActiveTrue(SystemRole.ADMIN) <= 1) {
            throw new BadRequestException("The last active admin cannot be removed or demoted");
        }
    }

    private void requireAdmin(User actor) {
        if (actor == null || actor.getSystemRole() != SystemRole.ADMIN) {
            throw new AccessDeniedCustomException("Admin role is required");
        }
    }

    private User findUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

}