package com.raffleease.raffleease.Domains.Auth.DTOs;

import com.raffleease.raffleease.Common.Validations.PasswordMatches;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import static com.raffleease.raffleease.Common.Constants.ValidationPatterns.PASSWORD_PATTERN;

@PasswordMatches
public record EditPasswordRequest(
        @NotBlank(message = "Current password is required")
        String currentPassword,

        @NotBlank
        @Pattern(regexp = PASSWORD_PATTERN)
        String password,

        @NotBlank
        @Pattern(regexp = PASSWORD_PATTERN)
        String confirmPassword
) { }