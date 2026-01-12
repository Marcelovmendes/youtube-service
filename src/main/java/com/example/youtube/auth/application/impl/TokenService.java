package com.example.youtube.auth.application.impl;

import com.example.youtube.auth.application.TokenQuery;
import com.example.youtube.auth.domain.entity.Token;
import com.example.youtube.auth.domain.repository.TokenRepository;
import com.example.youtube.auth.domain.service.OAuthClient;
import com.example.youtube.common.result.Error;
import com.example.youtube.common.result.Result;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class TokenService implements TokenQuery {

    private static final Logger log = LoggerFactory.getLogger(TokenService.class);

    private final HttpServletRequest request;
    private final TokenRepository tokenRepository;
    private final OAuthClient oauthClient;

    public TokenService(
            HttpServletRequest request,
            TokenRepository tokenRepository,
            OAuthClient oauthClient
    ) {
        this.request = request;
        this.tokenRepository = tokenRepository;
        this.oauthClient = oauthClient;
    }

    @Override
    public Result<Token, Error> getCurrentUserToken() {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String accessToken = authHeader.substring(7);
            log.debug("Using token from Authorization header");
            return Result.success(Token.fromAccessToken(accessToken));
        }

        HttpSession session = request.getSession(false);
        if (session == null) {
            log.warn("No session found when getting current user token");
            return Result.failure(Error.authenticationError(
                    "No active session",
                    "Please authenticate first"
            ));
        }

        String sessionId = session.getId();
        log.debug("Getting token for session: {}", sessionId);

        return tokenRepository.findBySessionId(sessionId)
                .flatMap(token -> {
                    if (!token.isValid()) {
                        return Result.failure(Error.authenticationError(
                                "Token is invalid or expired",
                                "Please re-authenticate"
                        ));
                    }
                    return Result.success(token);
                });
    }

    @Override
    public boolean isUserAuthenticated() {
        return getCurrentUserToken().isSuccess();
    }

    @Override
    public Result<Void, Error> storeUserToken(String sessionId, Token token) {
        log.info("Storing token for session: {}", sessionId);
        return tokenRepository.save(sessionId, token);
    }

    @Override
    public Result<Token, Error> refreshToken(String sessionId) {
        log.info("Refreshing token for session: {}", sessionId);

        return tokenRepository.findBySessionId(sessionId)
                .flatMap(token -> {
                    if (token.refreshToken() == null) {
                        return Result.failure(Error.authenticationError(
                                "No refresh token available",
                                "Token does not have a refresh token"
                        ));
                    }
                    return oauthClient.refreshToken(token.refreshToken());
                })
                .flatMap(newToken -> tokenRepository.save(sessionId, newToken)
                        .map(_ -> newToken));
    }

    @Override
    public Result<Void, Error> removeToken(String sessionId) {
        log.info("Removing token for session: {}", sessionId);
        return tokenRepository.remove(sessionId);
    }
}
