package com.example.youtube.auth.domain.entity;

import com.example.youtube.common.result.Error;
import com.example.youtube.common.result.Result;

import java.security.SecureRandom;
import java.util.Base64;

public final class AuthState {

    private final String stateValue;
    private final String codeVerifier;
    private final String codeChallenge;

    private AuthState(String stateValue, String codeVerifier, String codeChallenge) {
        this.stateValue = stateValue;
        this.codeVerifier = codeVerifier;
        this.codeChallenge = codeChallenge;
    }

    public static Result<AuthState, Error> create(String stateValue, String codeVerifier, String codeChallenge) {
        if (stateValue == null || stateValue.isBlank()) {
            return Result.failure(Error.invalidInputError("stateValue", "State value cannot be null or empty"));
        }

        if (codeVerifier == null || codeVerifier.isBlank()) {
            return Result.failure(Error.invalidInputError("codeVerifier", "Code verifier cannot be null or empty"));
        }

        if (codeChallenge == null || codeChallenge.isBlank()) {
            return Result.failure(Error.invalidInputError("codeChallenge", "Code challenge cannot be null or empty"));
        }

        return Result.success(new AuthState(stateValue, codeVerifier, codeChallenge));
    }

    public static String generateRandomState() {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public Result<Boolean, Error> validateState(String providedState) {
        if (providedState == null || providedState.isBlank()) {
            return Result.failure(Error.invalidStateError("Provided state is null or empty"));
        }

        if (!stateValue.equals(providedState)) {
            return Result.failure(Error.invalidStateError("State mismatch"));
        }

        return Result.success(true);
    }

    public String stateValue() {
        return stateValue;
    }

    public String codeVerifier() {
        return codeVerifier;
    }

    public String codeChallenge() {
        return codeChallenge;
    }
}
