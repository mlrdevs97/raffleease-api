package com.raffleease.raffleease.Common.RateLimiting;

import com.raffleease.raffleease.Domains.Users.Model.User;
import com.raffleease.raffleease.Domains.Users.Services.UsersService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import static com.raffleease.raffleease.Common.RateLimiting.RateLimit.AccessLevel.PRIVATE;

/**
 * Service for managing rate limiting using Redis-backed token bucket algorithm.
 * Provides access-level based rate limiting for different operations.
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class RateLimitingService {
    private final UsersService usersService;
    private final RedisTemplate<String, String> redisTemplate;
    private final RateLimitConfig rateLimitConfig;

    private static final String RATE_LIMIT_KEY_PREFIX = "rate_limit:";
    private static final String RATE_LIMIT_REFILL_KEY_PREFIX = "rate_limit_refill:";
    
    /**
     * Check if a request is allowed based on rate limiting rules
     */
    public boolean isRequestAllowed(String operation, RateLimit.AccessLevel accessLevel, Long associationId, boolean perUser) {
        try {
            String bucketKey = buildBucketKey(operation, accessLevel, associationId, perUser);
            RateLimitConfig.RateLimitRule rule = getRateLimitRule(operation, accessLevel);
            return consumeToken(bucketKey, rule);
        } catch (Exception e) {
            log.error("Error checking rate limit for operation {}: {}", operation, e.getMessage());
            return false;
        }
    }
    
    /**
     * Consume a token from the Redis-backed token bucket
     */
    private boolean consumeToken(String bucketKey, RateLimitConfig.RateLimitRule rule) {
        try {
            String tokenKey = RATE_LIMIT_KEY_PREFIX + bucketKey;
            String refillKey = RATE_LIMIT_REFILL_KEY_PREFIX + bucketKey;
            
            // Get current token count and last refill time
            String currentTokensStr = redisTemplate.opsForValue().get(tokenKey);
            String lastRefillStr = redisTemplate.opsForValue().get(refillKey);
            
            long currentTokens = currentTokensStr != null ? Long.parseLong(currentTokensStr) : rule.getCapacity();
            long lastRefillTime = lastRefillStr != null ? Long.parseLong(lastRefillStr) : Instant.now().toEpochMilli();
            
            // Calculate tokens to add based on time elapsed
            long now = Instant.now().toEpochMilli();
            long timeElapsed = now - lastRefillTime;
            long tokensToAdd = calculateTokensToAdd(timeElapsed, rule);
            
            // Refill tokens up to capacity
            long newTokenCount = Math.min(currentTokens + tokensToAdd, rule.getCapacity());
            
            // Check if a token can be consumed
            if (newTokenCount > 0) {
                // Consume one token
                newTokenCount--;
                
                // Update Redis with new values
                Duration ttl = rule.getRefillPeriod().multipliedBy(2);
                redisTemplate.opsForValue().set(tokenKey, String.valueOf(newTokenCount), ttl.toMillis(), TimeUnit.MILLISECONDS);
                redisTemplate.opsForValue().set(refillKey, String.valueOf(now), ttl.toMillis(), TimeUnit.MILLISECONDS);
                
                log.debug("Rate limit check passed for operation: {} (tokens remaining: {})", bucketKey, newTokenCount);
                return true;
            } else {
                // Update last refill time even if no tokens available
                redisTemplate.opsForValue().set(refillKey, String.valueOf(now), 
                                              rule.getRefillPeriod().multipliedBy(2).toMillis(), 
                                              TimeUnit.MILLISECONDS);
                
                String userInfo = getUserInfo();
                log.warn("Rate limit exceeded for user {} on operation: {}", userInfo, bucketKey);
                return false;
            }
            
        } catch (NumberFormatException e) {
            log.error("Invalid number format in Redis rate limit data for key: {}", bucketKey, e);
            return false;
        } catch (Exception e) {
            log.error("Redis error during rate limit check for key: {}", bucketKey, e);
            return false;
        }
    }
    
    /**
     * Calculate how many tokens to add based on elapsed time
     */
    private long calculateTokensToAdd(long timeElapsedMs, RateLimitConfig.RateLimitRule rule) {
        if (timeElapsedMs <= 0) {
            return 0;
        }
        
        double tokensPerMs = (double) rule.getRefillTokens() / rule.getRefillPeriod().toMillis();
        return (long) (timeElapsedMs * tokensPerMs);
    }
    
    /**
     * Get the appropriate rate limit rule for the operation and access level
     */
    private RateLimitConfig.RateLimitRule getRateLimitRule(String operation, RateLimit.AccessLevel accessLevel) {
        String accessPrefix = accessLevel.name().toLowerCase();
        String ruleKey = accessPrefix + "." + operation;
        RateLimitConfig.RateLimitRule rule = rateLimitConfig.getRateLimits().get(ruleKey);
        if (rule == null) {
            rule = rateLimitConfig.getRateLimits().get("general.api");
            log.debug("Using fallback rate limit for operation: {}", operation);
        }
        return rule;
    }
    
    /**
     * Build unique bucket key for the rate limiting scope
     */
    private String buildBucketKey(String operation, RateLimit.AccessLevel accessLevel, Long associationId, boolean perUser) {
        StringBuilder keyBuilder = new StringBuilder();
        keyBuilder.append(associationId != null ? associationId : "global").append(":");
        keyBuilder.append(accessLevel.name().toLowerCase()).append(":");
        keyBuilder.append(operation);
        
        if (perUser && accessLevel == PRIVATE) {
            try {
                User user = usersService.getAuthenticatedUser();
                keyBuilder.append(":").append(user.getId());
            } catch (Exception e) {
                log.debug("Could not get authenticated user for per-user rate limiting: {}", e.getMessage());
            }
        }
        return keyBuilder.toString();
    }
    
    /**
     * Get user info for logging
     */
    private String getUserInfo() {
        try {
            User user = usersService.getAuthenticatedUser();
            return user.getUserName();
        } catch (Exception e) {
            return "unknown";
        }
    }
    
    /**
     * Get current available tokens for monitoring/debugging
     */
    public long getAvailableTokens(String operation, RateLimit.AccessLevel accessLevel, 
                                  Long associationId, boolean perUser) {
        try {
            String bucketKey = buildBucketKey(operation, accessLevel, associationId, perUser);
            String tokenKey = RATE_LIMIT_KEY_PREFIX + bucketKey;
            
            String currentTokensStr = redisTemplate.opsForValue().get(tokenKey);
            if (currentTokensStr != null) {
                return Long.parseLong(currentTokensStr);
            }
            
            // If no key exists, return full capacity
            RateLimitConfig.RateLimitRule rule = getRateLimitRule(operation, accessLevel);
            return rule.getCapacity();
            
        } catch (Exception e) {
            log.error("Error getting available tokens for operation {}: {}", operation, e.getMessage());
            return 0;
        }
    }
    
    /**
     * Clear rate limit data for a specific key (useful for testing/admin operations)
     */
    public void clearRateLimit(String operation, RateLimit.AccessLevel accessLevel, 
                              Long associationId, boolean perUser) {
        try {
            String bucketKey = buildBucketKey(operation, accessLevel, associationId, perUser);
            String tokenKey = RATE_LIMIT_KEY_PREFIX + bucketKey;
            String refillKey = RATE_LIMIT_REFILL_KEY_PREFIX + bucketKey;
            
            redisTemplate.delete(tokenKey);
            redisTemplate.delete(refillKey);
            
            log.info("Cleared rate limit data for key: {}", bucketKey);
        } catch (Exception e) {
            log.error("Error clearing rate limit for operation {}: {}", operation, e.getMessage());
        }
    }
} 