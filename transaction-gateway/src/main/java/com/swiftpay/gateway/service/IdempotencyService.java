package com.swiftpay.gateway.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Redis-based idempotency — prevents same transaction from processing twice.
 * Key lives for 24h, after which the client can retry if needed.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class IdempotencyService {

    private static final String KEY_PREFIX = "idempotency:";

    private final StringRedisTemplate redisTemplate;

    @Value("${swiftpay.idempotency.ttl-hours:24}")
    private long ttlHours;

    /**
     * Try to acquire idempotency key. Returns true if new, false if duplicate.
     */
    public boolean tryAcquire(String idempotencyKey) {
        String redisKey = KEY_PREFIX + idempotencyKey;
        Boolean acquired = redisTemplate.opsForValue()
                .setIfAbsent(redisKey, "PROCESSED", Duration.ofHours(ttlHours));
        boolean result = Boolean.TRUE.equals(acquired);
        if (!result) {
            log.warn("duplicate request for key={}", idempotencyKey);
        }
        return result;
    }

    // cache sender balance briefly to reduce DB hits under load
    // 30s is probably too short but fine for now, can tune later
    public void cacheBalance(String userId, String balance) {
        String key = "balance:" + userId;
        redisTemplate.opsForValue().set(key, balance, Duration.ofSeconds(30));
    }

    public String getCachedBalance(String userId) {
        return redisTemplate.opsForValue().get("balance:" + userId);
    }

    public void evictBalance(String userId) {
        redisTemplate.delete("balance:" + userId);
    }
}
