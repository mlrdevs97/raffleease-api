package com.raffleease.raffleease.Common.Validations;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ValidOrderSearchFiltersValidator.class)
public @interface ValidOrderSearchFilters {
    String message() default "Invalid order search filters";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
