package com.raffleease.raffleease.Domains.Users.DTOs;

import com.raffleease.raffleease.Common.Models.UserRegisterDTO;
import com.raffleease.raffleease.Domains.Associations.Model.AssociationRole;
import com.raffleease.raffleease.Domains.Users.Validations.ValidUserRole;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

@Builder
public record CreateUserRequest(
        @NotNull
        @Valid
        UserRegisterDTO userData,
        
        @NotNull
        @ValidUserRole
        AssociationRole role
) {
} 