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
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class GoogleAuthenticationService implements AuthUseCase {

    private static final Duration STATE_TIMEOUT = Duration.ofMinutes(10);

    private final AuthStateRepository authStateRepository;
    private final TokenRepository tokenRepository;
    private final OAuthClient oauthClient;
    private final PkceGenerator pkceGenerator;
    private final HttpSession httpSession;

    public GoogleAuthenticationService(
            AuthStateRepository authStateRepository,
            TokenRepository tokenRepository,
            OAuthClient oauthClient,
            PkceGenerator pkceGenerator,
            HttpSession httpSession
    ) {
        this.authStateRepository = authStateRepository;
        this.tokenRepository = tokenRepository;
        this.oauthClient = oauthClient;
        this.pkceGenerator = pkceGenerator;
        this.httpSession = httpSession;
    }

    @Override
    public Result<AuthInitiationResponse, Error> initiateAuthentication() {
        return pkceGenerator.generate().flatMap(pkce -> {
            String stateValue = AuthState.generateRandomState();

            return AuthState.create(stateValue, pkce.codeVerifier(), pkce.codeChallenge())
                    .flatMap(authState -> authStateRepository.save(authState, STATE_TIMEOUT))
                    .flatMap(_ -> oauthClient.buildAuthorizationUrl(stateValue, pkce.codeChallenge()))
                    .map(AuthInitiationResponse::new);
        });
    }

    @Override
    public Result<Token, Error> handleCallback(AuthCallbackRequest request) {
        return authStateRepository.findByStateValue(request.state())
                .flatMap(authState -> authState.validateState(request.state())
                        .flatMap(_ -> oauthClient.exchangeCodeForToken(request.code(), authState.codeVerifier()))
                        .andThen(_ -> authStateRepository.remove(request.state())))
                .flatMap(token -> tokenRepository.save(httpSession.getId(), token).map(_ -> token));
    }

    @Override
    public Result<Token, Error> refreshToken(String sessionId) {
        return tokenRepository.findBySessionId(sessionId)
                .flatMap(token -> {
                    if (token.refreshToken() == null) {
                        return Result.failure(Error.authenticationError(
                                "No refresh token available",
                                "Token does not have a refresh token"));
                    }
                    return oauthClient.refreshToken(token.refreshToken());
                })
                .flatMap(newToken -> tokenRepository.save(sessionId, newToken).map(_ -> newToken));
    }
}
