package com.raffleease.raffleease.Domains.Auth.DTOs.Register;

import com.raffleease.raffleease.Common.Models.UserRegisterDTO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

@Builder
public record RegisterRequest(
        @NotNull
        @Valid
        UserRegisterDTO userData,

        @NotNull
        @Valid
        RegisterAssociationData associationData
) {}
