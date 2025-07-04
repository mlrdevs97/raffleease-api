package com.raffleease.raffleease.Common.Validations;

import com.raffleease.raffleease.Common.Models.UserRegisterDTO;
import com.raffleease.raffleease.Domains.Auth.DTOs.ResetPasswordRequest;
import com.raffleease.raffleease.Domains.Auth.DTOs.EditPasswordRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class PasswordMatchesValidator implements ConstraintValidator<PasswordMatches, Object> {

    @Override
    public boolean isValid(Object request, ConstraintValidatorContext context) {
        if (request == null) return true;

        String password;
        String confirmPassword;

        if (request instanceof UserRegisterDTO userData) {
            password = userData.getPassword();
            confirmPassword = userData.getConfirmPassword();
        } else if (request instanceof ResetPasswordRequest resetRequest) {
            password = resetRequest.password();
            confirmPassword = resetRequest.confirmPassword();
        } else if (request instanceof EditPasswordRequest editRequest) {
            password = editRequest.password();
            confirmPassword = editRequest.confirmPassword();
        } else {
            return true;
        }

        if (password == null || confirmPassword == null) return true;

        boolean passwordsMatch = password.equals(confirmPassword);
        if (!passwordsMatch) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("Password and confirm password don't match")
                    .addPropertyNode("confirmPassword").addConstraintViolation();
        }
        return passwordsMatch;
    }
}