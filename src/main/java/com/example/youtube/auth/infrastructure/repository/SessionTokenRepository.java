package com.example.youtube.auth.infrastructure.repository;

import com.example.youtube.auth.domain.entity.Token;
import com.example.youtube.auth.domain.repository.TokenRepository;
import com.example.youtube.common.result.Error;
import com.example.youtube.common.result.Result;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

@Repository
public class SessionTokenRepository implements TokenRepository {

    private static final Logger log = LoggerFactory.getLogger(SessionTokenRepository.class);

    private static final String ACCESS_TOKEN_KEY = "youtubeAccessToken";
    private static final String REFRESH_TOKEN_KEY = "youtubeRefreshToken";
    private static final String TOKEN_EXPIRY_KEY = "youtubeTokenExpiry";
    private static final String TOKEN_TYPE_KEY = "youtubeTokenType";

    private final HttpServletRequest request;

    public SessionTokenRepository(HttpServletRequest request) {
        this.request = request;
    }

    @Override
    public Result<Void, Error> save(String sessionId, Token token) {
        try {
            HttpSession session = request.getSession(false);
            if (session == null || !session.getId().equals(sessionId)) {
                session = request.getSession(true);
            }
            log.info("Saving token to session: {}", session.getId());
            session.setAttribute(ACCESS_TOKEN_KEY, token.accessToken());
            session.setAttribute(REFRESH_TOKEN_KEY, token.refreshToken());
            session.setAttribute(TOKEN_EXPIRY_KEY, token.expiresAt().toEpochMilli());
            session.setAttribute(TOKEN_TYPE_KEY, token.tokenType());
            return Result.successVoid();
        } catch (Exception e) {
            log.error("Failed to save token to session: {}", sessionId, e);
            return Result.failure(Error.externalServiceError("Session", "Failed to save token", e));
        }
    }

    @Override
    public Result<Token, Error> findBySessionId(String sessionId) {
        try {
            HttpSession session = request.getSession(false);
            if (session == null) {
                log.warn("No session found when looking for sessionId: {}", sessionId);
                return Result.failure(Error.resourceNotFoundError("Token", "No session"));
            }

            log.debug("Finding token in session: {}", session.getId());

            String accessToken = (String) session.getAttribute(ACCESS_TOKEN_KEY);
            String refreshToken = (String) session.getAttribute(REFRESH_TOKEN_KEY);
            Long expiryMillis = (Long) session.getAttribute(TOKEN_EXPIRY_KEY);
            String tokenType = (String) session.getAttribute(TOKEN_TYPE_KEY);

            if (accessToken == null || expiryMillis == null) {
                log.warn("Token not found in session: {}", session.getId());
                return Result.failure(Error.resourceNotFoundError("Token", session.getId()));
            }

            long expiresIn = (expiryMillis - System.currentTimeMillis()) / 1000;
            if (expiresIn <= 0) {
                log.warn("Token expired in session: {}", session.getId());
                return Result.failure(Error.resourceNotFoundError("Token", "Token expired"));
            }

            return Token.create(accessToken, refreshToken, expiresIn, tokenType);
        } catch (Exception e) {
            log.error("Failed to find token in session: {}", sessionId, e);
            return Result.failure(Error.externalServiceError("Session", "Failed to find token", e));
        }
    }

    @Override
    public Result<Void, Error> remove(String sessionId) {
        try {
            HttpSession session = request.getSession(false);
            if (session != null && session.getId().equals(sessionId)) {
                log.info("Removing token from session: {}", session.getId());
                session.removeAttribute(ACCESS_TOKEN_KEY);
                session.removeAttribute(REFRESH_TOKEN_KEY);
                session.removeAttribute(TOKEN_EXPIRY_KEY);
                session.removeAttribute(TOKEN_TYPE_KEY);
            }
            return Result.successVoid();
        } catch (Exception e) {
            log.error("Failed to remove token from session: {}", sessionId, e);
            return Result.failure(Error.externalServiceError("Session", "Failed to remove token", e));
        }
    }
}
