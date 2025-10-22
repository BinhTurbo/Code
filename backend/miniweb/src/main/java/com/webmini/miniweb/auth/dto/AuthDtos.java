package com.webmini.miniweb.auth.dto;

public class AuthDtos {
    public record LoginRequest(String username, String password) {}
    public record TokenResponse(
            String tokenType,
            String accessToken,
            long accessExpiresInSec,
            String refreshToken
    ) {}

    public record RefreshRequest(String refreshToken) {}

    public record MeResponse(Long id, String username, String fullName, String role) {}
}
