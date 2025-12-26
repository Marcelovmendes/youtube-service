package com.example.youtube.quota.infrastructure.repository;

import com.example.youtube.common.result.Error;
import com.example.youtube.common.result.Result;
import com.example.youtube.quota.domain.service.QuotaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneOffset;

@Service
public class RedisQuotaService implements QuotaService {

    private static final Logger log = LoggerFactory.getLogger(RedisQuotaService.class);
    private static final String QUOTA_KEY_PREFIX = "youtube:quota:";

    private final StringRedisTemplate redisTemplate;

    public RedisQuotaService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public Result<Void, Error> consumeQuota(int units) {
        return hasAvailableQuota(units).flatMap(hasQuota -> {
            if (!hasQuota) {
                return getCurrentUsage().flatMap(usage ->
                        Result.failure(Error.quotaExceededError(usage, DAILY_QUOTA_LIMIT)));
            }

            try {
                String key = buildDailyKey();
                Long newUsage = redisTemplate.opsForValue().increment(key, units);

                if (newUsage != null && newUsage == units) {
                    redisTemplate.expire(key, calculateTimeUntilMidnightPT());
                }

                log.info("Consumed {} quota units. Current usage: {}", units, newUsage);
                return Result.successVoid();
            } catch (Exception e) {
                log.error("Failed to consume quota", e);
                return Result.failure(Error.externalServiceError("Redis", "Failed to update quota", e));
            }
        });
    }

    @Override
    public Result<Long, Error> getCurrentUsage() {
        try {
            String key = buildDailyKey();
            String value = redisTemplate.opsForValue().get(key);
            long usage = value != null ? Long.parseLong(value) : 0L;
            return Result.success(usage);
        } catch (Exception e) {
            log.error("Failed to get current quota usage", e);
            return Result.failure(Error.externalServiceError("Redis", "Failed to retrieve quota usage", e));
        }
    }

    @Override
    public Result<Long, Error> getRemainingQuota() {
        return getCurrentUsage().map(usage -> Math.max(0, DAILY_QUOTA_LIMIT - usage));
    }

    @Override
    public Result<Boolean, Error> hasAvailableQuota(int requiredUnits) {
        return getCurrentUsage().map(usage -> (usage + requiredUnits) <= DAILY_QUOTA_LIMIT);
    }

    private String buildDailyKey() {
        LocalDate today = LocalDate.now(ZoneOffset.of("-08:00"));
        return QUOTA_KEY_PREFIX + today.toString();
    }

    private Duration calculateTimeUntilMidnightPT() {
        java.time.ZonedDateTime nowPT = java.time.ZonedDateTime.now(java.time.ZoneId.of("America/Los_Angeles"));
        java.time.ZonedDateTime midnightPT = nowPT.toLocalDate().plusDays(1).atStartOfDay(java.time.ZoneId.of("America/Los_Angeles"));
        return Duration.between(nowPT, midnightPT).plusMinutes(5);
    }
}
