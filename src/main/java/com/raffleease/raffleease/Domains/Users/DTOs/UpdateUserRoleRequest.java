package com.raffleease.raffleease.Domains.Users.DTOs;

import com.raffleease.raffleease.Domains.Associations.Model.AssociationRole;
import com.raffleease.raffleease.Domains.Users.Validations.ValidRoleUpdate;
import jakarta.validation.constraints.NotNull;

public record UpdateUserRoleRequest(
        @NotNull(message = "Role is required")
        @ValidRoleUpdate
        AssociationRole role
) {
} 