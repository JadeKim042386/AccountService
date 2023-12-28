package com.example.account.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class RedisLockRepository {
    private final RedisTemplate<String, String> redisTemplate;

    public Boolean lock(final String accountNumber) {
        return redisTemplate
                .opsForValue()
                .setIfAbsent(getLockKey(accountNumber), "lock", 1, TimeUnit.SECONDS);
    }

    public Boolean unlock(final String accountNumber) {
        return redisTemplate.delete(getLockKey(accountNumber));
    }

    public String getLockKey(String accountNumber) {
        return "ACLK:" + accountNumber;
    }
}
