package com.example.youtube.auth.infrastructure.repository;

import com.example.youtube.auth.domain.entity.AuthState;
import com.example.youtube.auth.domain.repository.AuthStateRepository;
import com.example.youtube.common.result.Error;
import com.example.youtube.common.result.Result;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;

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
                state.codeChallenge()
            ));
            redisTemplate.opsForValue().set(key, value, timeout);
            return Result.success(null);
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
            return AuthState.create(data.stateValue, data.codeVerifier, data.codeChallenge);
        } catch (Exception e) {
            return Result.failure(Error.externalServiceError("Redis", "Failed to find auth state", e));
        }
    }

    @Override
    public Result<Void, Error> remove(String stateValue) {
        try {
            String key = KEY_PREFIX + stateValue;
            redisTemplate.delete(key);
            return Result.success(null);
        } catch (Exception e) {
            return Result.failure(Error.externalServiceError("Redis", "Failed to remove auth state", e));
        }
    }

    private record StateData(String stateValue, String codeVerifier, String codeChallenge) {}
}
