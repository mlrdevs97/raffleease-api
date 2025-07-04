package com.raffleease.raffleease.Domains.Users.DTOs;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record UpdateEmailRequest(
        @NotBlank
        @Email
        String newEmail
) {
} 