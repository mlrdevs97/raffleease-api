package com.raffleease.raffleease.Common.Aspects;

import com.raffleease.raffleease.Common.Exceptions.CustomExceptions.RateLimitExceededException;
import com.raffleease.raffleease.Common.RateLimiting.RateLimit;
import com.raffleease.raffleease.Common.RateLimiting.RateLimitingService;
import com.raffleease.raffleease.Common.Utils.AspectUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

/**
 * Aspect for handling rate limiting annotations.
 * Intercepts methods annotated with @RateLimit and applies throttling logic.
 */
@Slf4j
@RequiredArgsConstructor
@Aspect
@Component
public class RateLimitingAspect {
    
    private final RateLimitingService rateLimitingService;
    
    @Around("@annotation(rateLimit)")
    public Object checkRateLimit(ProceedingJoinPoint joinPoint, RateLimit rateLimit) throws Throwable {
        Long associationId = AspectUtils.extractAssociationId(joinPoint);
        
        boolean allowed = rateLimitingService.isRequestAllowed(
            rateLimit.operation(), 
            rateLimit.accessLevel(),
            associationId, 
            rateLimit.perUser()
        );
        
        if (!allowed) {
            log.warn("Rate limit exceeded for {} operation: {}", 
                    rateLimit.accessLevel().name().toLowerCase(), rateLimit.operation());
            throw new RateLimitExceededException(rateLimit.message());
        }
        
        log.debug("Rate limit check passed for {} operation: {}", 
                rateLimit.accessLevel().name().toLowerCase(), rateLimit.operation());
        return joinPoint.proceed();
    }
} 