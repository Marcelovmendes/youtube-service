package com.example.youtube.auth.domain.entity;

import com.example.youtube.common.result.Error;
import com.example.youtube.common.result.Result;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class TokenTest {

    @Test
    void shouldCreateValidToken() {
        var result = Token.create("access123", "refresh123", 3600L, "Bearer");

        assertThat(result.isSuccess()).isTrue();

        var token = switch (result) {
            case Result.Success(var value) -> value;
            case Result.Failure(_) -> null;
        };

        assertThat(token).isNotNull();
        assertThat(token.accessToken()).isEqualTo("access123");
        assertThat(token.refreshToken()).isEqualTo("refresh123");
        assertThat(token.tokenType()).isEqualTo("Bearer");
        assertThat(token.expiresAt()).isAfter(Instant.now());
        assertThat(token.isValid()).isTrue();
        assertThat(token.isExpired()).isFalse();
    }

    @Test
    void shouldFailWhenAccessTokenIsNull() {
        var result = Token.create(null, "refresh123", 3600L, "Bearer");

        assertThat(result.isFailure()).isTrue();

        var error = switch (result) {
            case Result.Success(_) -> null;
            case Result.Failure(var err) -> err;
        };

        assertThat(error).isInstanceOf(Error.InvalidInputError.class);
        var invalidInputError = (Error.InvalidInputError) error;
        assertThat(invalidInputError.field()).isEqualTo("accessToken");
    }

    @Test
    void shouldFailWhenAccessTokenIsBlank() {
        var result = Token.create("   ", "refresh123", 3600L, "Bearer");

        assertThat(result.isFailure()).isTrue();
    }

    @Test
    void shouldFailWhenExpiresInIsNull() {
        var result = Token.create("access123", "refresh123", null, "Bearer");

        assertThat(result.isFailure()).isTrue();

        var error = switch (result) {
            case Result.Success(_) -> null;
            case Result.Failure(var err) -> err;
        };

        assertThat(error).isInstanceOf(Error.InvalidInputError.class);
        var invalidInputError = (Error.InvalidInputError) error;
        assertThat(invalidInputError.field()).isEqualTo("expiresIn");
    }

    @Test
    void shouldFailWhenExpiresInIsNegative() {
        var result = Token.create("access123", "refresh123", -100L, "Bearer");

        assertThat(result.isFailure()).isTrue();
    }

    @Test
    void shouldFailWhenExpiresInIsZero() {
        var result = Token.create("access123", "refresh123", 0L, "Bearer");

        assertThat(result.isFailure()).isTrue();
    }

    @Test
    void shouldBeInvalidWhenExpired() {
        var result = Token.create("access123", "refresh123", -1L, "Bearer");

        assertThat(result.isFailure()).isTrue();
    }

    @Test
    void shouldBeInvalidWhenAccessTokenIsEmpty() {
        var validResult = Token.create("access123", "refresh123", 3600L, "Bearer");
        assertThat(validResult.isSuccess()).isTrue();

        var emptyTokenResult = Token.create("", "refresh123", 3600L, "Bearer");
        assertThat(emptyTokenResult.isFailure()).isTrue();
    }
}
