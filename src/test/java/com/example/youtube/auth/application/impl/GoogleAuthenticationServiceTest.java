package com.example.youtube.auth.application.impl;

import com.example.youtube.auth.application.AuthUseCase;
import com.example.youtube.auth.domain.entity.AuthState;
import com.example.youtube.auth.domain.entity.Token;
import com.example.youtube.auth.domain.repository.AuthStateRepository;
import com.example.youtube.auth.domain.service.OAuthClient;
import com.example.youtube.auth.domain.service.PkceGenerator;
import com.example.youtube.common.result.Error;
import com.example.youtube.common.result.Result;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GoogleAuthenticationServiceTest {

    @Mock
    private AuthStateRepository authStateRepository;

    @Mock
    private OAuthClient oauthClient;

    @Mock
    private PkceGenerator pkceGenerator;

    private GoogleAuthenticationService authenticationService;

    @BeforeEach
    void setUp() {
        authenticationService = new GoogleAuthenticationService(
                authStateRepository,
                oauthClient,
                pkceGenerator
        );
    }

    @Nested
    class InitiateAuthentication {

        @Test
        void shouldSucceed() {
            var pkceChallenge = new PkceGenerator.PkceChallenge("verifier123", "challenge123");
            when(pkceGenerator.generate()).thenReturn(Result.success(pkceChallenge));
            when(authStateRepository.save(any(AuthState.class), any(Duration.class))).thenReturn(Result.successVoid());
            when(oauthClient.buildAuthorizationUrl(anyString(), eq("challenge123")))
                    .thenReturn(Result.success("https://accounts.google.com/auth?code_challenge=challenge123"));

            var result = authenticationService.initiateAuthentication();

            assertThat(result.isSuccess()).isTrue();
            String url = result.fold(AuthUseCase.AuthInitiationResponse::authorizationUrl, err -> null);
            assertThat(url).contains("https://accounts.google.com/auth");

            verify(pkceGenerator).generate();
            verify(authStateRepository).save(any(AuthState.class), eq(Duration.ofMinutes(10)));
            verify(oauthClient).buildAuthorizationUrl(anyString(), eq("challenge123"));
        }

        @Test
        void shouldFailWhenPkceGenerationFails() {
            when(pkceGenerator.generate())
                    .thenReturn(Result.failure(Error.externalServiceError("PKCE", "Failed to generate", null)));

            var result = authenticationService.initiateAuthentication();

            assertThat(result.isFailure()).isTrue();
            verify(pkceGenerator).generate();
            verifyNoInteractions(authStateRepository, oauthClient);
        }

        @Test
        void shouldFailWhenSaveAuthStateFails() {
            var pkceChallenge = new PkceGenerator.PkceChallenge("verifier123", "challenge123");
            when(pkceGenerator.generate()).thenReturn(Result.success(pkceChallenge));
            when(authStateRepository.save(any(AuthState.class), any(Duration.class)))
                    .thenReturn(Result.failure(Error.externalServiceError("Redis", "Connection failed", null)));

            var result = authenticationService.initiateAuthentication();

            assertThat(result.isFailure()).isTrue();
            Error error = result.fold(success -> null, err -> err);
            assertThat(error).isInstanceOf(Error.ExternalServiceError.class);
            verifyNoInteractions(oauthClient);
        }

        @Test
        void shouldFailWhenBuildAuthorizationUrlFails() {
            var pkceChallenge = new PkceGenerator.PkceChallenge("verifier123", "challenge123");
            when(pkceGenerator.generate()).thenReturn(Result.success(pkceChallenge));
            when(authStateRepository.save(any(AuthState.class), any(Duration.class))).thenReturn(Result.successVoid());
            when(oauthClient.buildAuthorizationUrl(anyString(), anyString()))
                    .thenReturn(Result.failure(Error.externalServiceError("Google", "Invalid config", null)));

            var result = authenticationService.initiateAuthentication();

            assertThat(result.isFailure()).isTrue();
            Error error = result.fold(success -> null, err -> err);
            assertThat(error).isInstanceOf(Error.ExternalServiceError.class);
        }
    }

    @Nested
    class ExchangeCodeForToken {

        private AuthState validAuthState;
        private Token validToken;

        @BeforeEach
        void setUp() {
            validAuthState = AuthState.create("state123", "verifier123", "challenge123")
                    .fold(s -> s, _ -> null);
            validToken = Token.create("access123", "refresh123", 3600L, "Bearer")
                    .fold(t -> t, _ -> null);
        }

        @Test
        void shouldSucceed() {
            when(authStateRepository.findByStateValue("state123")).thenReturn(Result.success(validAuthState));
            when(oauthClient.exchangeCodeForToken("code123", "verifier123")).thenReturn(Result.success(validToken));
            when(authStateRepository.markAsProcessed("state123", validToken)).thenReturn(Result.successVoid());

            var request = new AuthUseCase.AuthCallbackRequest("code123", "state123");
            var result = authenticationService.exchangeCodeForToken(request);

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.fold(Token::accessToken, err -> null)).isEqualTo("access123");

            verify(authStateRepository).findByStateValue("state123");
            verify(oauthClient).exchangeCodeForToken("code123", "verifier123");
            verify(authStateRepository).markAsProcessed("state123", validToken);
        }

        @Test
        void shouldFailWhenStateNotFound() {
            when(authStateRepository.findByStateValue("invalidState"))
                    .thenReturn(Result.failure(Error.resourceNotFoundError("AuthState", "invalidState")));

            var request = new AuthUseCase.AuthCallbackRequest("code123", "invalidState");
            var result = authenticationService.exchangeCodeForToken(request);

            assertThat(result.isFailure()).isTrue();
            Error error = result.fold(success -> null, err -> err);
            assertThat(error).isInstanceOf(Error.ResourceNotFoundError.class);
            verifyNoInteractions(oauthClient);
        }

        @Test
        void shouldFailWhenExchangeCodeFails() {
            when(authStateRepository.findByStateValue("state123")).thenReturn(Result.success(validAuthState));
            when(oauthClient.exchangeCodeForToken("code123", "verifier123"))
                    .thenReturn(Result.failure(Error.tokenExchangeError("Invalid code", "Code expired")));

            var request = new AuthUseCase.AuthCallbackRequest("code123", "state123");
            var result = authenticationService.exchangeCodeForToken(request);

            assertThat(result.isFailure()).isTrue();
            Error error = result.fold(success -> null, err -> err);
            assertThat(error).isInstanceOf(Error.TokenExchangeError.class);
            verify(authStateRepository, never()).markAsProcessed(anyString(), any(Token.class));
        }

        @Test
        void shouldReturnCachedTokenWhenStateAlreadyProcessed() {
            var processedAuthState = validAuthState.withProcessedToken(validToken);
            when(authStateRepository.findByStateValue("state123")).thenReturn(Result.success(processedAuthState));

            var request = new AuthUseCase.AuthCallbackRequest("code123", "state123");
            var result = authenticationService.exchangeCodeForToken(request);

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.fold(Token::accessToken, err -> null)).isEqualTo("access123");

            verify(authStateRepository).findByStateValue("state123");
            verifyNoInteractions(oauthClient);
            verify(authStateRepository, never()).markAsProcessed(anyString(), any(Token.class));
        }
    }
}
