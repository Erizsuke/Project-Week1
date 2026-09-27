package com.yourorg.taskmanager.auth.service;

import com.yourorg.taskmanager.auth.entity.SystemRole;
import com.yourorg.taskmanager.auth.entity.User;
import com.yourorg.taskmanager.auth.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Component
public class AdminAccountBootstrap implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String email;
    private final String password;
    private final String fullName;

    public AdminAccountBootstrap(UserRepository userRepository, PasswordEncoder passwordEncoder,
            @Value("${app.bootstrap.admin-email:}") String email,
            @Value("${app.bootstrap.admin-password:}") String password,
            @Value("${app.bootstrap.admin-name:System Administrator}") String fullName) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.email = email.trim().toLowerCase(Locale.ROOT);
        this.password = password;
        this.fullName = fullName.trim();
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (email.isBlank() || password.isBlank()) {
            return;
        }
        if (password.length() < 8) {
            throw new IllegalStateException("Bootstrap admin password must be at least 8 characters");
        }

        User placeholderAdmin = userRepository.findAll().stream()
                .filter(user -> user.getSystemRole() == SystemRole.ADMIN && user.isActive())
                .filter(user -> user.getPasswordHash().contains("REPLACE_WITH_YOUR_BCRYPT_HASH"))
                .findFirst().orElse(null);
        if (placeholderAdmin != null) {
            userRepository.findByEmail(email).filter(existing -> !existing.getId().equals(placeholderAdmin.getId()))
                    .ifPresent(existing -> { throw new IllegalStateException("Bootstrap admin email is already in use"); });
            placeholderAdmin.setEmail(email);
            placeholderAdmin.setFullName(fullName);
            placeholderAdmin.setPasswordHash(passwordEncoder.encode(password));
            userRepository.save(placeholderAdmin);
            return;
        }

        if (userRepository.countBySystemRoleAndActiveTrue(SystemRole.ADMIN) > 0
                || userRepository.existsByEmail(email)) {
            return;
        }
        User admin = new User();
        admin.setEmail(email);
        admin.setFullName(fullName);
        admin.setPasswordHash(passwordEncoder.encode(password));
        admin.setSystemRole(SystemRole.ADMIN);
        admin.setActive(true);
        userRepository.save(admin);
    }
}