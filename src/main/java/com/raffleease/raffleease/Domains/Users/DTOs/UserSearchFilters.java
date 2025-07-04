package com.raffleease.raffleease.Domains.Users.DTOs;

import com.raffleease.raffleease.Domains.Associations.Model.AssociationRole;
import jakarta.annotation.Nullable;
import jakarta.validation.Valid;

import static com.raffleease.raffleease.Common.Utils.SanitizeUtils.trim;
import static com.raffleease.raffleease.Common.Utils.SanitizeUtils.trimAndLower;

public record UserSearchFilters(
    @Nullable
    String fullName,

    @Nullable
    String email,
    
    @Nullable
    @Valid
    String phoneNumber,
    
    @Nullable
    AssociationRole role
) {
    public UserSearchFilters {
        fullName = trim(fullName);
        email = trimAndLower(email);
        phoneNumber = trim(phoneNumber);
    }
} 