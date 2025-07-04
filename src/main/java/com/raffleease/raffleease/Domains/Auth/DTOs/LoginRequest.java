package com.raffleease.raffleease.Domains.Auth.DTOs;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

import static com.raffleease.raffleease.Common.Utils.SanitizeUtils.trimAndLower;

@Builder
public record LoginRequest(
        @NotBlank
        String identifier,
        @NotBlank
        String password
) {
        public LoginRequest {
                identifier = trimAndLower(identifier);
        }
}
