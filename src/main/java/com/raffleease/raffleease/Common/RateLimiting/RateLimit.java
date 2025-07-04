package com.raffleease.raffleease.Common.RateLimiting;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static com.raffleease.raffleease.Common.RateLimiting.RateLimit.AccessLevel.PRIVATE;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Annotation to apply rate limiting to controller methods.
 * Rate limits are determined by the operation type and access level (public/private).
 */
@Target({METHOD})
@Retention(RUNTIME)
public @interface RateLimit {
    
    /**
     * The operation type (e.g., "create", "update", "delete", "upload")
     */
    String operation();
    
    /**
     * Access level for the endpoint (public or private)
     */
    AccessLevel accessLevel() default PRIVATE;
    
    /**
     * Fallback limit to use if specific limit is not found
     */
    int fallbackLimit() default 100;
    
    /**
     * Custom message to be shown when rate limit is exceeded
     */
    String message() default "Rate limit exceeded. Please try again later.";
    
    /**
     * Whether to apply rate limiting per user or globally for the operation
     */
    boolean perUser() default true;
    
    /**
     * Access level enumeration
     */
    enum AccessLevel {
        PUBLIC,
        PRIVATE
    }
} 