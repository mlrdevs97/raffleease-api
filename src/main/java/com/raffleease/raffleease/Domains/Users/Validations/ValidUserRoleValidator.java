package com.raffleease.raffleease.Domains.Users.Validations;

import com.raffleease.raffleease.Domains.Associations.Model.AssociationRole;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class ValidUserRoleValidator implements ConstraintValidator<ValidUserRole, AssociationRole> {
    @Override
    public void initialize(ValidUserRole constraintAnnotation) {}

    @Override
    public boolean isValid(AssociationRole role, ConstraintValidatorContext context) {
        if (role == null) {
            return true; 
        }
        return role == AssociationRole.MEMBER || role == AssociationRole.COLLABORATOR || role == AssociationRole.ADMIN;
    }
} 