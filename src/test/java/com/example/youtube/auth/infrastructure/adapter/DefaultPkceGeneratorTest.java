package com.example.youtube.auth.infrastructure.adapter;

import com.example.youtube.auth.domain.service.PkceGenerator;
import com.example.youtube.common.result.Result;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultPkceGeneratorTest {

    private DefaultPkceGenerator pkceGenerator;

    @BeforeEach
    void setUp() {
        pkceGenerator = new DefaultPkceGenerator();
    }

    @Test
    void shouldGenerateValidPkceChallenge() {
        var result = pkceGenerator.generate();

        assertThat(result.isSuccess()).isTrue();

        var pkce = switch (result) {
            case Result.Success(var value) -> value;
            case Result.Failure(_) -> null;
        };

        assertThat(pkce).isNotNull();
        assertThat(pkce.codeVerifier()).isNotNull();
        assertThat(pkce.codeVerifier()).isNotBlank();
        assertThat(pkce.codeChallenge()).isNotNull();
        assertThat(pkce.codeChallenge()).isNotBlank();
    }

    @Test
    void shouldGenerateUniqueChallenges() {
        var result1 = pkceGenerator.generate();
        var result2 = pkceGenerator.generate();

        assertThat(result1.isSuccess()).isTrue();
        assertThat(result2.isSuccess()).isTrue();

        var pkce1 = switch (result1) {
            case Result.Success(var value) -> value;
            case Result.Failure(_) -> null;
        };

        var pkce2 = switch (result2) {
            case Result.Success(var value) -> value;
            case Result.Failure(_) -> null;
        };

        assertThat(pkce1).isNotNull();
        assertThat(pkce2).isNotNull();
        assertThat(pkce1.codeVerifier()).isNotEqualTo(pkce2.codeVerifier());
        assertThat(pkce1.codeChallenge()).isNotEqualTo(pkce2.codeChallenge());
    }

    @Test
    void shouldGenerateDifferentChallengeFromVerifier() {
        var result = pkceGenerator.generate();

        assertThat(result.isSuccess()).isTrue();

        var pkce = switch (result) {
            case Result.Success(var value) -> value;
            case Result.Failure(_) -> null;
        };

        assertThat(pkce).isNotNull();
        assertThat(pkce.codeVerifier()).isNotEqualTo(pkce.codeChallenge());
    }

    @Test
    void shouldGenerateUrlSafeStrings() {
        var result = pkceGenerator.generate();

        assertThat(result.isSuccess()).isTrue();

        var pkce = switch (result) {
            case Result.Success(var value) -> value;
            case Result.Failure(_) -> null;
        };

        assertThat(pkce).isNotNull();
        assertThat(pkce.codeVerifier()).matches("[A-Za-z0-9_-]+");
        assertThat(pkce.codeChallenge()).matches("[A-Za-z0-9_-]+");
    }
}
