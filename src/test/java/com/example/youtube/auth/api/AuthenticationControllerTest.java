package com.example.youtube.auth.api;

import com.example.youtube.auth.application.AuthUseCase;
import com.example.youtube.auth.domain.entity.Token;
import com.example.youtube.common.result.Error;
import com.example.youtube.common.result.Result;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthenticationController.class)
class AuthenticationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthUseCase authUseCase;

    @Nested
    class HandleCallback {

        @Test
        void shouldReturn200WhenSuccess() throws Exception {
            var token = Token.create("access123", "refresh123", 3600L, "Bearer")
                    .fold(t -> t, err -> null);
            when(authUseCase.handleCallback(any())).thenReturn(Result.success(token));

            mockMvc.perform(get("/api/auth/google/callback")
                            .param("code", "validCode")
                            .param("state", "validState"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accessToken").value("access123"));
        }

        @Test
        void shouldReturn401WhenUserDeniesAuthorization() throws Exception {
            mockMvc.perform(get("/api/auth/google/callback")
                            .param("code", "code")
                            .param("state", "state")
                            .param("error", "access_denied"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.type").value("AUTHORIZATION_DENIED"));
        }

        @Test
        void shouldReturn404WhenStateNotFound() throws Exception {
            when(authUseCase.handleCallback(any()))
                    .thenReturn(Result.failure(Error.resourceNotFoundError("AuthState", "invalidState")));

            mockMvc.perform(get("/api/auth/google/callback")
                            .param("code", "validCode")
                            .param("state", "invalidState"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.type").value("RESOURCE_NOT_FOUND"));
        }

        @Test
        void shouldReturn401WhenTokenExchangeFails() throws Exception {
            when(authUseCase.handleCallback(any()))
                    .thenReturn(Result.failure(Error.tokenExchangeError("Invalid code", "Code expired")));

            mockMvc.perform(get("/api/auth/google/callback")
                            .param("code", "expiredCode")
                            .param("state", "validState"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.type").value("TOKEN_EXCHANGE_FAILED"));
        }

        @Test
        void shouldReturn503WhenRedisUnavailable() throws Exception {
            when(authUseCase.handleCallback(any()))
                    .thenReturn(Result.failure(Error.externalServiceError("Redis", "Connection refused", null)));

            mockMvc.perform(get("/api/auth/google/callback")
                            .param("code", "validCode")
                            .param("state", "validState"))
                    .andExpect(status().isServiceUnavailable())
                    .andExpect(jsonPath("$.type").value("EXTERNAL_SERVICE_ERROR"))
                    .andExpect(jsonPath("$.details").value("Service: Redis"));
        }
    }

    @Nested
    class RefreshToken {

        @Test
        void shouldReturn200WhenSuccess() throws Exception {
            var token = Token.create("newAccess", "refresh123", 3600L, "Bearer")
                    .fold(t -> t, err -> null);
            when(authUseCase.refreshToken(anyString())).thenReturn(Result.success(token));

            mockMvc.perform(post("/api/auth/refresh"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accessToken").value("newAccess"));
        }

        @Test
        void shouldReturn401WhenNoRefreshToken() throws Exception {
            when(authUseCase.refreshToken(anyString()))
                    .thenReturn(Result.failure(Error.authenticationError(
                            "No refresh token available",
                            "Token does not have a refresh token")));

            mockMvc.perform(post("/api/auth/refresh"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.type").value("AUTHENTICATION_ERROR"));
        }

        @Test
        void shouldReturn404WhenTokenNotFound() throws Exception {
            when(authUseCase.refreshToken(anyString()))
                    .thenReturn(Result.failure(Error.resourceNotFoundError("Token", "session123")));

            mockMvc.perform(post("/api/auth/refresh"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.type").value("RESOURCE_NOT_FOUND"));
        }

        @Test
        void shouldReturn503WhenGoogleApiUnavailable() throws Exception {
            when(authUseCase.refreshToken(anyString()))
                    .thenReturn(Result.failure(Error.externalServiceError("Google OAuth", "Service unavailable", null)));

            mockMvc.perform(post("/api/auth/refresh"))
                    .andExpect(status().isServiceUnavailable())
                    .andExpect(jsonPath("$.type").value("EXTERNAL_SERVICE_ERROR"))
                    .andExpect(jsonPath("$.details").value("Service: Google OAuth"));
        }

        @Test
        void shouldReturn503WhenRedisUnavailable() throws Exception {
            when(authUseCase.refreshToken(anyString()))
                    .thenReturn(Result.failure(Error.externalServiceError("Redis", "Connection timeout", null)));

            mockMvc.perform(post("/api/auth/refresh"))
                    .andExpect(status().isServiceUnavailable())
                    .andExpect(jsonPath("$.type").value("EXTERNAL_SERVICE_ERROR"))
                    .andExpect(jsonPath("$.details").value("Service: Redis"));
        }
    }

    @Nested
    class InitiateAuthentication {

        @Test
        void shouldReturn200WhenSuccess() throws Exception {
            var response = new AuthUseCase.AuthInitiationResponse("https://accounts.google.com/auth");
            when(authUseCase.initiateAuthentication()).thenReturn(Result.success(response));

            mockMvc.perform(get("/api/auth"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.authorizationUrl").value("https://accounts.google.com/auth"));
        }

        @Test
        void shouldReturn503WhenPkceGenerationFails() throws Exception {
            when(authUseCase.initiateAuthentication())
                    .thenReturn(Result.failure(Error.externalServiceError("PKCE", "SecureRandom failed", null)));

            mockMvc.perform(get("/api/auth"))
                    .andExpect(status().isServiceUnavailable())
                    .andExpect(jsonPath("$.type").value("EXTERNAL_SERVICE_ERROR"));
        }

        @Test
        void shouldReturn503WhenRedisUnavailable() throws Exception {
            when(authUseCase.initiateAuthentication())
                    .thenReturn(Result.failure(Error.externalServiceError("Redis", "Connection refused", null)));

            mockMvc.perform(get("/api/auth"))
                    .andExpect(status().isServiceUnavailable())
                    .andExpect(jsonPath("$.type").value("EXTERNAL_SERVICE_ERROR"))
                    .andExpect(jsonPath("$.details").value("Service: Redis"));
        }
    }
}
