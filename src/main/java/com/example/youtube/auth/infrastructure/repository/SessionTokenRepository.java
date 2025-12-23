package com.example.youtube.auth.infrastructure.repository;

import com.example.youtube.auth.domain.entity.Token;
import com.example.youtube.auth.domain.repository.TokenRepository;
import com.example.youtube.common.result.Error;
import com.example.youtube.common.result.Result;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Repository;

@Repository
public class SessionTokenRepository implements TokenRepository {

    private static final String TOKEN_SESSION_KEY = "youtube_token";

    private final HttpSession httpSession;
    private final ObjectMapper objectMapper;

    public SessionTokenRepository(HttpSession httpSession, ObjectMapper objectMapper) {
        this.httpSession = httpSession;
        this.objectMapper = objectMapper;
    }

    @Override
    public Result<Void, Error> save(String sessionId, Token token) {
        try {
            TokenData data = new TokenData(
                token.accessToken(),
                token.refreshToken(),
                token.expiresAt().getEpochSecond(),
                token.tokenType()
            );
            httpSession.setAttribute(TOKEN_SESSION_KEY, objectMapper.writeValueAsString(data));
            return Result.success(null);
        } catch (Exception e) {
            return Result.failure(Error.externalServiceError("Session", "Failed to save token", e));
        }
    }

    @Override
    public Result<Token, Error> findBySessionId(String sessionId) {
        try {
            String value = (String) httpSession.getAttribute(TOKEN_SESSION_KEY);

            if (value == null) {
                return Result.failure(Error.resourceNotFoundError("Token", sessionId));
            }

            TokenData data = objectMapper.readValue(value, TokenData.class);
            return Token.create(data.accessToken, data.refreshToken, data.expiresIn, data.tokenType);
        } catch (Exception e) {
            return Result.failure(Error.externalServiceError("Session", "Failed to find token", e));
        }
    }

    @Override
    public Result<Void, Error> remove(String sessionId) {
        try {
            httpSession.removeAttribute(TOKEN_SESSION_KEY);
            return Result.success(null);
        } catch (Exception e) {
            return Result.failure(Error.externalServiceError("Session", "Failed to remove token", e));
        }
    }

    private record TokenData(String accessToken, String refreshToken, Long expiresIn, String tokenType) {}
}
