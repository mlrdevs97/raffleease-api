package com.raffleease.raffleease.Domains.Auth.DTOs;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

@Builder
public record RegisterEmailVerificationRequest(
        @NotBlank
        String verificationToken
) {
}
