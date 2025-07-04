package com.raffleease.raffleease.Domains.Users.DTOs;

import jakarta.validation.constraints.NotBlank;

public record VerifyEmailUpdateRequest(
        @NotBlank
        String token
) {
} 