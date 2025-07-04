package com.raffleease.raffleease.Common.RateLimiting;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Configuration class for rate limiting based on endpoint access level and CRUD operations.
 * Uses Redis-backed token bucket algorithm for flexible rate limiting.
 */
@Component
@Configuration
public class RateLimitConfig {
    
    @Value("${rate-limiting.limits.private.create:50}")
    private int privateCreateRateLimit;

    @Value("${rate-limiting.limits.private.read:300}")
    private int privateReadRateLimit;
    
    @Value("${rate-limiting.limits.private.update:100}")
    private int privateUpdateRateLimit;

    @Value("${rate-limiting.limits.private.delete:30}")
    private int privateDeleteRateLimit;

    @Value("${rate-limiting.limits.private.upload:50}")
    private int privateUploadRateLimit;

    @Value("${rate-limiting.limits.public.search:500}")
    private int publicSearchRateLimit;

    @Value("${rate-limiting.limits.public.read:1000}")
    private int publicReadRateLimit;

    @Value("${rate-limiting.limits.general.api:100}")
    private int generalApiRateLimit;

    @Value("${rate-limiting.burst.private:15}")
    private int privateBurstLimit;

    @Value("${rate-limiting.burst.public:10}")
    private int publicBurstLimit;

    @Value("${rate-limiting.burst.general:5}")
    private int generalBurstLimit;

    private Map<String, RateLimitRule> rateLimits;
    private Map<String, Integer> burstLimits;
    
    @PostConstruct
    public void initializeMaps() {
        rateLimits = new HashMap<>();
        rateLimits.put("private.create", new RateLimitRule(privateCreateRateLimit, Duration.ofHours(1)));
        rateLimits.put("private.read", new RateLimitRule(privateReadRateLimit, Duration.ofHours(1)));
        rateLimits.put("private.update", new RateLimitRule(privateUpdateRateLimit, Duration.ofHours(1)));
        rateLimits.put("private.delete", new RateLimitRule(privateDeleteRateLimit, Duration.ofHours(1)));
        rateLimits.put("private.upload", new RateLimitRule(privateUploadRateLimit, Duration.ofHours(1)));
        rateLimits.put("private.test", new RateLimitRule(privateUploadRateLimit, Duration.ofMinutes(1)));
        rateLimits.put("public.search", new RateLimitRule(publicSearchRateLimit, Duration.ofHours(1)));
        rateLimits.put("public.read", new RateLimitRule(publicReadRateLimit, Duration.ofHours(1)));
        rateLimits.put("general.api", new RateLimitRule(generalApiRateLimit, Duration.ofHours(1)));
        
        burstLimits = new HashMap<>();
        burstLimits.put("private", privateBurstLimit);
        burstLimits.put("public", publicBurstLimit);
        burstLimits.put("general", generalBurstLimit);
    }

    public Map<String, RateLimitRule> getRateLimits() { return rateLimits; }
    public Map<String, Integer> getBurstLimits() {
        return burstLimits;
    }

    /**
     * Rate limit rule definition
     */
    public static class RateLimitRule {
        private final long capacity;
        private final Duration refillPeriod;
        private final long refillTokens;
        
        public RateLimitRule(long capacity, Duration refillPeriod) {
            this.capacity = capacity;
            this.refillPeriod = refillPeriod;
            this.refillTokens = capacity;
        }
        
        public long getCapacity() { return capacity; }
        public Duration getRefillPeriod() { return refillPeriod; }
        public long getRefillTokens() { return refillTokens; }
    }
} 