package com.example.youtube.auth.infrastructure.repository;

import com.example.youtube.auth.domain.entity.AuthState;
import com.example.youtube.auth.domain.entity.Token;
import com.example.youtube.auth.domain.repository.AuthStateRepository;
import com.example.youtube.common.result.Error;
import com.example.youtube.common.result.Result;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

@Repository
public class RedisAuthStateRepository implements AuthStateRepository {

    private static final String KEY_PREFIX = "oauth:state:";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public RedisAuthStateRepository(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public Result<Void, Error> save(AuthState state, Duration timeout) {
        try {
            String key = KEY_PREFIX + state.stateValue();
            String value = objectMapper.writeValueAsString(new StateData(
                state.stateValue(),
                state.codeVerifier(),
                state.codeChallenge(),
                null,
                null,
                null,
                null
            ));
            redisTemplate.opsForValue().set(key, value, timeout);
            return Result.successVoid();
        } catch (Exception e) {
            return Result.failure(Error.externalServiceError("Redis", "Failed to save auth state", e));
        }
    }

    @Override
    public Result<AuthState, Error> findByStateValue(String stateValue) {
        try {
            String key = KEY_PREFIX + stateValue;
            String value = redisTemplate.opsForValue().get(key);

            if (value == null) {
                return Result.failure(Error.resourceNotFoundError("AuthState", stateValue));
            }

            StateData data = objectMapper.readValue(value, StateData.class);
            return AuthState.create(data.stateValue, data.codeVerifier, data.codeChallenge)
                    .map(authState -> {
                        if (data.accessToken != null) {
                            Token token = Token.fromStoredData(
                                    data.accessToken,
                                    data.refreshToken,
                                    Instant.parse(data.expiresAt),
                                    data.tokenType
                            );
                            return authState.withProcessedToken(token);
                        }
                        return authState;
                    });
        } catch (Exception e) {
            return Result.failure(Error.externalServiceError("Redis", "Failed to find auth state", e));
        }
    }

    @Override
    public Result<Void, Error> markAsProcessed(String stateValue, Token token) {
        try {
            String key = KEY_PREFIX + stateValue;
            Long ttl = redisTemplate.getExpire(key);
            if (ttl == null || ttl < 0) {
                return Result.failure(Error.resourceNotFoundError("AuthState", stateValue));
            }

            String existingValue = redisTemplate.opsForValue().get(key);
            if (existingValue == null) {
                return Result.failure(Error.resourceNotFoundError("AuthState", stateValue));
            }

            StateData existingData = objectMapper.readValue(existingValue, StateData.class);
            StateData updatedData = new StateData(
                    existingData.stateValue,
                    existingData.codeVerifier,
                    existingData.codeChallenge,
                    token.accessToken(),
                    token.refreshToken(),
                    token.expiresAt().toString(),
                    token.tokenType()
            );

            String value = objectMapper.writeValueAsString(updatedData);
            redisTemplate.opsForValue().set(key, value, Duration.ofSeconds(ttl));
            return Result.successVoid();
        } catch (Exception e) {
            return Result.failure(Error.externalServiceError("Redis", "Failed to mark auth state as processed", e));
        }
    }

    private record StateData(
            String stateValue,
            String codeVerifier,
            String codeChallenge,
            String accessToken,
            String refreshToken,
            String expiresAt,
            String tokenType
    ) {
        private StateData {
            Objects.requireNonNull(stateValue, "stateValue");
            Objects.requireNonNull(codeVerifier, "codeVerifier");
            Objects.requireNonNull(codeChallenge, "codeChallenge");
        }
    }
}
