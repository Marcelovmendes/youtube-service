package com.example.youtube.auth.application.impl;

import com.example.youtube.auth.application.AuthUseCase;
import com.example.youtube.auth.domain.entity.AuthState;
import com.example.youtube.auth.domain.entity.Token;
import com.example.youtube.auth.domain.repository.AuthStateRepository;
import com.example.youtube.auth.domain.repository.TokenRepository;
import com.example.youtube.auth.domain.service.OAuthClient;
import com.example.youtube.auth.domain.service.PkceGenerator;
import com.example.youtube.common.result.Error;
import com.example.youtube.common.result.Result;
import jakarta.servlet.http.HttpSession;
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
    private TokenRepository tokenRepository;

    @Mock
    private OAuthClient oauthClient;

    @Mock
    private PkceGenerator pkceGenerator;

    @Mock
    private HttpSession httpSession;

    private GoogleAuthenticationService authenticationService;

    @BeforeEach
    void setUp() {
        authenticationService = new GoogleAuthenticationService(
                authStateRepository,
                tokenRepository,
                oauthClient,
                pkceGenerator,
                httpSession
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
    class HandleCallback {

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
            when(httpSession.getId()).thenReturn("session123");
            when(authStateRepository.findByStateValue("state123")).thenReturn(Result.success(validAuthState));
            when(oauthClient.exchangeCodeForToken("code123", "verifier123")).thenReturn(Result.success(validToken));
            when(authStateRepository.remove("state123")).thenReturn(Result.successVoid());
            when(tokenRepository.save("session123", validToken)).thenReturn(Result.successVoid());

            var request = new AuthUseCase.AuthCallbackRequest("code123", "state123");
            var result = authenticationService.handleCallback(request);

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.fold(Token::accessToken, err -> null)).isEqualTo("access123");

            verify(authStateRepository).findByStateValue("state123");
            verify(oauthClient).exchangeCodeForToken("code123", "verifier123");
            verify(authStateRepository).remove("state123");
            verify(tokenRepository).save("session123", validToken);
        }

        @Test
        void shouldFailWhenStateNotFound() {
            when(authStateRepository.findByStateValue("invalidState"))
                    .thenReturn(Result.failure(Error.resourceNotFoundError("AuthState", "invalidState")));

            var request = new AuthUseCase.AuthCallbackRequest("code123", "invalidState");
            var result = authenticationService.handleCallback(request);

            assertThat(result.isFailure()).isTrue();
            Error error = result.fold(success -> null, err -> err);
            assertThat(error).isInstanceOf(Error.ResourceNotFoundError.class);
            verifyNoInteractions(oauthClient, tokenRepository);
        }

        @Test
        void shouldFailWhenExchangeCodeFails() {
            when(authStateRepository.findByStateValue("state123")).thenReturn(Result.success(validAuthState));
            when(oauthClient.exchangeCodeForToken("code123", "verifier123"))
                    .thenReturn(Result.failure(Error.tokenExchangeError("Invalid code", "Code expired")));

            var request = new AuthUseCase.AuthCallbackRequest("code123", "state123");
            var result = authenticationService.handleCallback(request);

            assertThat(result.isFailure()).isTrue();
            Error error = result.fold(success -> null, err -> err);
            assertThat(error).isInstanceOf(Error.TokenExchangeError.class);
            verify(authStateRepository, never()).remove(anyString());
            verifyNoInteractions(tokenRepository);
        }

        @Test
        void shouldFailWhenRemoveStateFails() {
            when(authStateRepository.findByStateValue("state123")).thenReturn(Result.success(validAuthState));
            when(oauthClient.exchangeCodeForToken("code123", "verifier123")).thenReturn(Result.success(validToken));
            when(authStateRepository.remove("state123"))
                    .thenReturn(Result.failure(Error.externalServiceError("Redis", "Connection lost", null)));

            var request = new AuthUseCase.AuthCallbackRequest("code123", "state123");
            var result = authenticationService.handleCallback(request);

            assertThat(result.isFailure()).isTrue();
            Error error = result.fold(success -> null, err -> err);
            assertThat(error).isInstanceOf(Error.ExternalServiceError.class);
            verifyNoInteractions(tokenRepository);
        }

        @Test
        void shouldFailWhenSaveTokenFails() {
            when(httpSession.getId()).thenReturn("session123");
            when(authStateRepository.findByStateValue("state123")).thenReturn(Result.success(validAuthState));
            when(oauthClient.exchangeCodeForToken("code123", "verifier123")).thenReturn(Result.success(validToken));
            when(authStateRepository.remove("state123")).thenReturn(Result.successVoid());
            when(tokenRepository.save("session123", validToken))
                    .thenReturn(Result.failure(Error.externalServiceError("Session", "Session expired", null)));

            var request = new AuthUseCase.AuthCallbackRequest("code123", "state123");
            var result = authenticationService.handleCallback(request);

            assertThat(result.isFailure()).isTrue();
            Error error = result.fold(success -> null, err -> err);
            assertThat(error).isInstanceOf(Error.ExternalServiceError.class);
        }
    }

    @Nested
    class RefreshToken {

        @Test
        void shouldSucceed() {
            var oldToken = Token.create("oldAccess", "refresh123", 3600L, "Bearer").fold(t -> t, _ -> null);
            var newToken = Token.create("newAccess", "refresh123", 3600L, "Bearer").fold(t -> t, _ -> null);

            when(tokenRepository.findBySessionId("session123")).thenReturn(Result.success(oldToken));
            when(oauthClient.refreshToken("refresh123")).thenReturn(Result.success(newToken));
            when(tokenRepository.save("session123", newToken)).thenReturn(Result.successVoid());

            var result = authenticationService.refreshToken("session123");

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.fold(Token::accessToken, err -> null)).isEqualTo("newAccess");

            verify(tokenRepository).findBySessionId("session123");
            verify(oauthClient).refreshToken("refresh123");
            verify(tokenRepository).save("session123", newToken);
        }

        @Test
        void shouldFailWhenNoRefreshToken() {
            var token = Token.create("access123", null, 3600L, "Bearer").fold(t -> t, _ -> null);
            when(tokenRepository.findBySessionId("session123")).thenReturn(Result.success(token));

            var result = authenticationService.refreshToken("session123");

            assertThat(result.isFailure()).isTrue();
            Error error = result.fold(success -> null, err -> err);
            assertThat(error).isInstanceOf(Error.AuthenticationError.class);
            verifyNoInteractions(oauthClient);
        }

        @Test
        void shouldFailWhenTokenNotFound() {
            when(tokenRepository.findBySessionId("invalidSession"))
                    .thenReturn(Result.failure(Error.resourceNotFoundError("Token", "invalidSession")));

            var result = authenticationService.refreshToken("invalidSession");

            assertThat(result.isFailure()).isTrue();
            Error error = result.fold(success -> null, err -> err);
            assertThat(error).isInstanceOf(Error.ResourceNotFoundError.class);
            verifyNoInteractions(oauthClient);
        }

        @Test
        void shouldFailWhenOAuthRefreshFails() {
            var oldToken = Token.create("oldAccess", "refresh123", 3600L, "Bearer").fold(t -> t, _ -> null);
            when(tokenRepository.findBySessionId("session123")).thenReturn(Result.success(oldToken));
            when(oauthClient.refreshToken("refresh123"))
                    .thenReturn(Result.failure(Error.tokenExchangeError("Refresh failed", "Token revoked")));

            var result = authenticationService.refreshToken("session123");

            assertThat(result.isFailure()).isTrue();
            Error error = result.fold(success -> null, err -> err);
            assertThat(error).isInstanceOf(Error.TokenExchangeError.class);
            verify(tokenRepository, never()).save(anyString(), any());
        }

        @Test
        void shouldFailWhenSaveNewTokenFails() {
            var oldToken = Token.create("oldAccess", "refresh123", 3600L, "Bearer").fold(t -> t, _ -> null);
            var newToken = Token.create("newAccess", "refresh123", 3600L, "Bearer").fold(t -> t, _ -> null);

            when(tokenRepository.findBySessionId("session123")).thenReturn(Result.success(oldToken));
            when(oauthClient.refreshToken("refresh123")).thenReturn(Result.success(newToken));
            when(tokenRepository.save("session123", newToken))
                    .thenReturn(Result.failure(Error.externalServiceError("Session", "Write failed", null)));

            var result = authenticationService.refreshToken("session123");

            assertThat(result.isFailure()).isTrue();
            Error error = result.fold(success -> null, err -> err);
            assertThat(error).isInstanceOf(Error.ExternalServiceError.class);
        }
    }
}
