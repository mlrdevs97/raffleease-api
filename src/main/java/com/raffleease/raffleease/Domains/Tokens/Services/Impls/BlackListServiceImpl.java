package com.raffleease.raffleease.Domains.Tokens.Services.Impls;

import com.raffleease.raffleease.Domains.Tokens.Services.BlackListService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

@RequiredArgsConstructor
@Service
public class BlackListServiceImpl implements BlackListService {
    private final RedisTemplate<String, String> redisTemplate;

    @Override
    public void addTokenToBlackList(String id, Long expiration) {
        if (Objects.isNull(id) || id.trim().isEmpty()) {
            throw new IllegalArgumentException("Token ID must not be null or empty");
        }

        if (Objects.isNull(expiration) || expiration <= 0) {
            throw new IllegalArgumentException("Expiration must be a positive number");
        }

        try {
            redisTemplate.opsForValue().set(id, "blacklisted", expiration, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            throw new RuntimeException("Failed to add token to blacklist", e);
        }
    }

    @Override
    public boolean isTokenBlackListed(String id) {
        if (Objects.isNull(id) || id.trim().isEmpty()) {
            throw new IllegalArgumentException("Token ID must not be null or empty");
        }
        return Boolean.TRUE.equals(redisTemplate.hasKey(id));
    }
}