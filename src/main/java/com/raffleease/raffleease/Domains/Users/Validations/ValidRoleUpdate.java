package com.raffleease.raffleease.Domains.Users.Validations;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Documented
@Constraint(validatedBy = ValidRoleUpdateValidator.class)
@Target({FIELD, PARAMETER})
@Retention(RUNTIME)
public @interface ValidRoleUpdate {
    String message() default "Only MEMBER and COLLABORATOR roles are allowed for role updates";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
} 