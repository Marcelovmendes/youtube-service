package com.example.youtube.auth.application.impl;

import com.example.youtube.auth.application.AuthUseCase;
import com.example.youtube.auth.domain.entity.AuthState;
import com.example.youtube.auth.domain.entity.Token;
import com.example.youtube.auth.domain.repository.AuthStateRepository;
import com.example.youtube.auth.domain.service.OAuthClient;
import com.example.youtube.auth.domain.service.PkceGenerator;
import com.example.youtube.common.result.Error;
import com.example.youtube.common.result.Result;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class GoogleAuthenticationService implements AuthUseCase {

    private static final Duration STATE_TIMEOUT = Duration.ofMinutes(10);

    private final AuthStateRepository authStateRepository;
    private final OAuthClient oauthClient;
    private final PkceGenerator pkceGenerator;

    public GoogleAuthenticationService(
            AuthStateRepository authStateRepository,
            OAuthClient oauthClient,
            PkceGenerator pkceGenerator
    ) {
        this.authStateRepository = authStateRepository;
        this.oauthClient = oauthClient;
        this.pkceGenerator = pkceGenerator;
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
    public Result<Token, Error> exchangeCodeForToken(AuthCallbackRequest request) {
        return authStateRepository.findByStateValue(request.state())
                .flatMap(authState -> {
                    if (authState.isProcessed()) {
                        return Result.success(authState.processedToken());
                    }

                    return authState.validateState(request.state())
                            .flatMap(_ -> oauthClient.exchangeCodeForToken(request.code(), authState.codeVerifier()))
                            .andThen(token -> authStateRepository.markAsProcessed(request.state(), token));
                });
    }
}
