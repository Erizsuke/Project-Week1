package com.yourorg.taskmanager.auth.security;

import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class RefreshTokenUtil {
    public String generateToken() {
        return UUID.randomUUID().toString() + UUID.randomUUID();
    }
}
