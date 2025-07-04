package com.raffleease.raffleease.Domains.Users.Validations;

import com.raffleease.raffleease.Domains.Associations.Model.AssociationRole;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class ValidRoleUpdateValidator implements ConstraintValidator<ValidRoleUpdate, AssociationRole> {
    @Override
    public void initialize(ValidRoleUpdate constraintAnnotation) {}

    @Override
    public boolean isValid(AssociationRole role, ConstraintValidatorContext context) {
        if (role == null) {
            return true;
        }    
        return role == AssociationRole.MEMBER || role == AssociationRole.COLLABORATOR;
    }
} 