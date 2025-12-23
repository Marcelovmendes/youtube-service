package com.example.youtube.auth.domain.entity;

import com.example.youtube.common.result.Error;
import com.example.youtube.common.result.Result;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AuthStateTest {

    @Test
    void shouldCreateValidAuthState() {
        var result = AuthState.create("state123", "verifier456", "challenge789");

        assertThat(result.isSuccess()).isTrue();

        var authState = switch (result) {
            case Result.Success(var value) -> value;
            case Result.Failure(_) -> null;
        };

        assertThat(authState).isNotNull();
        assertThat(authState.stateValue()).isEqualTo("state123");
        assertThat(authState.codeVerifier()).isEqualTo("verifier456");
        assertThat(authState.codeChallenge()).isEqualTo("challenge789");
    }

    @Test
    void shouldFailWhenStateValueIsNull() {
        var result = AuthState.create(null, "verifier456", "challenge789");

        assertThat(result.isFailure()).isTrue();

        var error = switch (result) {
            case Result.Success(_) -> null;
            case Result.Failure(var err) -> err;
        };

        assertThat(error).isInstanceOf(Error.InvalidInputError.class);
        var invalidInputError = (Error.InvalidInputError) error;
        assertThat(invalidInputError.field()).isEqualTo("stateValue");
    }

    @Test
    void shouldFailWhenStateValueIsBlank() {
        var result = AuthState.create("  ", "verifier456", "challenge789");

        assertThat(result.isFailure()).isTrue();
    }

    @Test
    void shouldFailWhenCodeVerifierIsNull() {
        var result = AuthState.create("state123", null, "challenge789");

        assertThat(result.isFailure()).isTrue();

        var error = switch (result) {
            case Result.Success(_) -> null;
            case Result.Failure(var err) -> err;
        };

        assertThat(error).isInstanceOf(Error.InvalidInputError.class);
        var invalidInputError = (Error.InvalidInputError) error;
        assertThat(invalidInputError.field()).isEqualTo("codeVerifier");
    }

    @Test
    void shouldFailWhenCodeChallengeIsNull() {
        var result = AuthState.create("state123", "verifier456", null);

        assertThat(result.isFailure()).isTrue();

        var error = switch (result) {
            case Result.Success(_) -> null;
            case Result.Failure(var err) -> err;
        };

        assertThat(error).isInstanceOf(Error.InvalidInputError.class);
        var invalidInputError = (Error.InvalidInputError) error;
        assertThat(invalidInputError.field()).isEqualTo("codeChallenge");
    }

    @Test
    void shouldValidateStateSuccessfully() {
        var authState = switch (AuthState.create("state123", "verifier456", "challenge789")) {
            case Result.Success(var value) -> value;
            case Result.Failure(_) -> null;
        };

        assertThat(authState).isNotNull();

        var validationResult = authState.validateState("state123");

        assertThat(validationResult.isSuccess()).isTrue();

        var isValid = switch (validationResult) {
            case Result.Success(var value) -> value;
            case Result.Failure(_) -> false;
        };

        assertThat(isValid).isTrue();
    }

    @Test
    void shouldFailValidationWhenStateDoesNotMatch() {
        var authState = switch (AuthState.create("state123", "verifier456", "challenge789")) {
            case Result.Success(var value) -> value;
            case Result.Failure(_) -> null;
        };

        assertThat(authState).isNotNull();

        var validationResult = authState.validateState("wrongState");

        assertThat(validationResult.isFailure()).isTrue();

        var error = switch (validationResult) {
            case Result.Success(_) -> null;
            case Result.Failure(var err) -> err;
        };

        assertThat(error).isInstanceOf(Error.InvalidStateError.class);
    }

    @Test
    void shouldFailValidationWhenProvidedStateIsNull() {
        var authState = switch (AuthState.create("state123", "verifier456", "challenge789")) {
            case Result.Success(var value) -> value;
            case Result.Failure(_) -> null;
        };

        assertThat(authState).isNotNull();

        var validationResult = authState.validateState(null);

        assertThat(validationResult.isFailure()).isTrue();
    }

    @Test
    void shouldGenerateRandomState() {
        String state1 = AuthState.generateRandomState();
        String state2 = AuthState.generateRandomState();

        assertThat(state1).isNotNull();
        assertThat(state1).isNotBlank();
        assertThat(state2).isNotNull();
        assertThat(state2).isNotBlank();
        assertThat(state1).isNotEqualTo(state2);
    }
}
