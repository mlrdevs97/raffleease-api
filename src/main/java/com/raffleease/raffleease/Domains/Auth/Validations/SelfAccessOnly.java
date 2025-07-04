package com.raffleease.raffleease.Domains.Auth.Validations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Annotation to mark methods that can only be accessed by the user themselves.
 * Unlike @RequireRole with allowSelfAccess=true, this prevents even administrators 
 * from accessing other users' resources.
 */
@Target({METHOD})
@Retention(RUNTIME)
public @interface SelfAccessOnly {
    String userIdParam() default "userId";
    String message() default "You can only access your own account for this action";
} 