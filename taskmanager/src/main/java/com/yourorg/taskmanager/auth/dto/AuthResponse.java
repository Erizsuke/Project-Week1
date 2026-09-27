package com.yourorg.taskmanager.auth.dto;

import java.util.UUID;

public class AuthResponse {

    private String accessToken;
    private String refreshToken;
    private UUID userId;
    private String email;
    private String systemRole;

    public AuthResponse(String accessToken, String refreshToken, UUID userId, String email, String systemRole) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.userId = userId;
        this.email = email;
        this.systemRole = systemRole;
    }

    public String getAccessToken() { return accessToken; }
    public void setAccessToken(String accessToken) { this.accessToken = accessToken; }

    public String getRefreshToken() { return refreshToken; }
    public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getSystemRole() { return systemRole; }
    public void setSystemRole(String systemRole) { this.systemRole = systemRole; }
}
