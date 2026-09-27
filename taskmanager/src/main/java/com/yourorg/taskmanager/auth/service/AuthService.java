package com.yourorg.taskmanager.auth.service;

import com.yourorg.taskmanager.auth.dto.AuthResponse;
import com.yourorg.taskmanager.auth.dto.LoginRequest;
import com.yourorg.taskmanager.auth.dto.RefreshRequest;
import com.yourorg.taskmanager.auth.dto.RegisterRequest;
import com.yourorg.taskmanager.auth.entity.RefreshToken;
import com.yourorg.taskmanager.auth.entity.SystemRole;
import com.yourorg.taskmanager.auth.entity.User;
import com.yourorg.taskmanager.auth.repository.RefreshTokenRepository;
import com.yourorg.taskmanager.auth.repository.UserRepository;
import com.yourorg.taskmanager.auth.security.JwtUtil;
import com.yourorg.taskmanager.auth.security.RefreshTokenUtil;
import com.yourorg.taskmanager.common.exception.AccessDeniedCustomException;
import com.yourorg.taskmanager.common.exception.BadRequestException;
import com.yourorg.taskmanager.common.exception.DuplicateResourceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Locale;

@Service
public class AuthService {

    private static final Logger LOG = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final RefreshTokenUtil refreshTokenUtil;

    @Value("${jwt.refresh-token-expiration}")
    private long refreshTokenExpiration;

    public AuthService(UserRepository userRepository,
                        RefreshTokenRepository refreshTokenRepository,
                        PasswordEncoder passwordEncoder,
                        JwtUtil jwtUtil,
                        RefreshTokenUtil refreshTokenUtil) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.refreshTokenUtil = refreshTokenUtil;
    }

    @Transactional
    public AuthResponse register(RegisterRequest req) {
        String email = normalizeEmail(req.getEmail());
        if (userRepository.existsByEmail(email)) {
            throw new DuplicateResourceException("Email da duoc su dung");
        }

        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(req.getPassword()));
        user.setFullName(req.getFullName());
        user.setSystemRole(SystemRole.USER);
        user.setActive(true);

        userRepository.save(user);
        LOG.info("New user registered: {}", user.getEmail());

        return buildAuthResponse(user);
    }

    @Transactional
    public AuthResponse login(LoginRequest req) {
        User user = userRepository.findByEmail(normalizeEmail(req.getEmail()))
                .orElseThrow(() -> new BadRequestException("Email hoac mat khau khong dung"));

        if (!user.isActive()) {
            throw new BadRequestException("Tai khoan da bi vo hieu hoa");
        }

        if (!passwordEncoder.matches(req.getPassword(), user.getPasswordHash())) {
            LOG.warn("Failed login attempt for email: {}", req.getEmail());
            throw new BadRequestException("Email hoac mat khau khong dung");
        }

        LOG.info("User logged in: {}", user.getEmail());
        return buildAuthResponse(user);
    }

    @Transactional
    public AuthResponse refresh(RefreshRequest req) {
        RefreshToken storedToken = refreshTokenRepository.findByToken(req.getRefreshToken())
                .orElseThrow(() -> new AccessDeniedCustomException("Refresh token khong hop le"));

        if (storedToken.isRevoked() || storedToken.getExpiresAt().isBefore(Instant.now())) {
            throw new AccessDeniedCustomException("Refresh token da het han hoac bi thu hoi");
        }

        User user = storedToken.getUser();
        if (!user.isActive()) {
            throw new AccessDeniedCustomException("Tai khoan da bi vo hieu hoa");
        }

        storedToken.setRevoked(true);
        refreshTokenRepository.save(storedToken);

        LOG.info("Token refreshed for user: {}", user.getEmail());
        return buildAuthResponse(user);
    }

    @Transactional
    public void logout(String refreshToken) {
        refreshTokenRepository.findByToken(refreshToken)
                .ifPresent(t -> {
                    t.setRevoked(true);
                    refreshTokenRepository.save(t);
                });
    }

    private AuthResponse buildAuthResponse(User user) {
        String accessToken = jwtUtil.generateAccessToken(user);

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setToken(refreshTokenUtil.generateToken());
        refreshToken.setExpiresAt(Instant.now().plusMillis(refreshTokenExpiration));
        refreshTokenRepository.save(refreshToken);

        return new AuthResponse(
                accessToken,
                refreshToken.getToken(),
                user.getId(),
                user.getEmail(),
                user.getSystemRole().name()
        );
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
