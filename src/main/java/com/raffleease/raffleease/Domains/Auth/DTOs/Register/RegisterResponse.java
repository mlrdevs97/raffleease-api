package com.raffleease.raffleease.Domains.Auth.DTOs.Register;

import lombok.Builder;

@Builder
public record RegisterResponse(
    Long id,
    String email
) {} 