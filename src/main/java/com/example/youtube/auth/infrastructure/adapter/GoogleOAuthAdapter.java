package com.example.youtube.auth.infrastructure.adapter;

import com.example.youtube.auth.domain.entity.Token;
import com.example.youtube.auth.domain.service.OAuthClient;
import com.example.youtube.common.config.GoogleApiConfig;
import com.example.youtube.common.result.Error;
import com.example.youtube.common.result.Result;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeTokenRequest;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
public class GoogleOAuthAdapter implements OAuthClient {

    private final GoogleApiConfig config;
    private final NetHttpTransport httpTransport;
    private final JsonFactory jsonFactory;

    public GoogleOAuthAdapter(GoogleApiConfig config, NetHttpTransport httpTransport, JsonFactory jsonFactory) {
        this.config = config;
        this.httpTransport = httpTransport;
        this.jsonFactory = jsonFactory;
    }

    @Override
    public Result<String, Error> buildAuthorizationUrl(String state, String codeChallenge) {
        try {
            List<String> scopes = Arrays.asList(config.getScopes().split(" "));

            GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                    httpTransport,
                    jsonFactory,
                    config.getClientId(),
                    config.getClientSecret(),
                    scopes
            ).build();

            String url = flow.newAuthorizationUrl()
                    .setRedirectUri(config.getRedirectUri())
                    .setState(state)
                    .set("code_challenge", codeChallenge)
                    .set("code_challenge_method", "S256")
                    .setAccessType("offline")
                    .build();

            return Result.success(url);
        } catch (Exception e) {
            return Result.failure(Error.externalServiceError("Google OAuth", "Failed to build authorization URL", e));
        }
    }

    @Override
    public Result<Token, Error> exchangeCodeForToken(String code, String codeVerifier) {
        try {
            GoogleTokenResponse response = new GoogleAuthorizationCodeTokenRequest(
                    httpTransport,
                    jsonFactory,
                    config.getClientId(),
                    config.getClientSecret(),
                    code,
                    config.getRedirectUri()
            ).set("code_verifier", codeVerifier).execute();

            return Token.create(
                    response.getAccessToken(),
                    response.getRefreshToken(),
                    response.getExpiresInSeconds(),
                    response.getTokenType()
            );
        } catch (Exception e) {
            return Result.failure(Error.tokenExchangeError("Failed to exchange code for token", e.getMessage()));
        }
    }

    @Override
    public Result<Token, Error> refreshToken(String refreshToken) {
        try {
            GoogleTokenResponse response = new GoogleAuthorizationCodeTokenRequest(
                    httpTransport,
                    jsonFactory,
                    config.getClientId(),
                    config.getClientSecret(),
                    "",
                    config.getRedirectUri()
            ).set("refresh_token", refreshToken)
             .set("grant_type", "refresh_token")
             .execute();

            return Token.create(
                    response.getAccessToken(),
                    response.getRefreshToken() != null ? response.getRefreshToken() : refreshToken,
                    response.getExpiresInSeconds(),
                    response.getTokenType()
            );
        } catch (Exception e) {
            return Result.failure(Error.tokenExchangeError("Failed to refresh token", e.getMessage()));
        }
    }
}
