package com.example.youtube.auth.domain.entity;

import com.example.youtube.common.result.Error;
import com.example.youtube.common.result.Result;

import java.time.Instant;

public final class Token {

    private final String accessToken;
    private final String refreshToken;
    private final Instant expiresAt;
    private final String tokenType;

    private Token(String accessToken, String refreshToken, Instant expiresAt, String tokenType) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.expiresAt = expiresAt;
        this.tokenType = tokenType;
    }

    public static Result<Token, Error> create(String accessToken, String refreshToken, Long expiresIn, String tokenType) {
        if (accessToken == null || accessToken.isBlank()) {
            return Result.failure(Error.invalidInputError("accessToken", "Access token cannot be null or empty"));
        }

        if (expiresIn == null || expiresIn <= 0) {
            return Result.failure(Error.invalidInputError("expiresIn", "Expires in must be positive"));
        }

        Instant expiresAt = Instant.now().plusSeconds(expiresIn);
        return Result.success(new Token(accessToken, refreshToken, expiresAt, tokenType));
    }

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    public boolean isValid() {
        return !isExpired() && accessToken != null && !accessToken.isBlank();
    }

    public String accessToken() {
        return accessToken;
    }

    public String refreshToken() {
        return refreshToken;
    }

    public Instant expiresAt() {
        return expiresAt;
    }

    public String tokenType() {
        return tokenType;
    }
}
