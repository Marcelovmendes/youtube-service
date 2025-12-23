package com.example.youtube.auth.api.dto;

import com.example.youtube.auth.domain.entity.Token;

import java.time.Instant;

public record TokenResponse(
        String accessToken,
        String tokenType,
        Instant expiresAt,
        boolean isValid
) {
    public static TokenResponse fromToken(Token token) {
        return new TokenResponse(
                token.accessToken(),
                token.tokenType(),
                token.expiresAt(),
                token.isValid()
        );
    }
}
