package com.raffleease.raffleease.Domains.Auth.Validations;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Annotation to mark methods that can only be accessed by ADMIN users.
 * This is a shorthand for @RequireRole(ADMIN)
 */
@Target({METHOD, TYPE})
@Retention(RUNTIME)
public @interface AdminOnly {
    String message() default "This action can only be performed by administrators";
} 