package com.raffleease.raffleease.Domains.Auth.Validations;

import com.raffleease.raffleease.Domains.Associations.Model.AssociationRole;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Annotation to specify the minimum role required to access a method or class.
 * Roles are hierarchical: ADMIN > MEMBER > COLLABORATOR
 */
@Target({METHOD, TYPE})
@Retention(RUNTIME)
public @interface RequireRole {
    AssociationRole value();
    
    /**
     * When true, allows users to access their own resources even if they don't have the required role.
     * For example, a COLLABORATOR can edit their own profile even if the endpoint requires ADMIN role.
     */
    boolean allowSelfAccess() default false;

    /**
     * Custom message to be shown when access is denied
     */
    String message() default "Insufficient permissions to access this resource";
} 