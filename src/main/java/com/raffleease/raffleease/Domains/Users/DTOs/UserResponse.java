package com.raffleease.raffleease.Domains.Users.DTOs;

import com.raffleease.raffleease.Common.Models.PhoneNumberDTO;
import com.raffleease.raffleease.Domains.Associations.Model.AssociationRole;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record UserResponse(
        Long id,
        String firstName,
        String lastName,
        String userName,
        String email,
        PhoneNumberDTO phoneNumber,
        AssociationRole role,
        boolean isEnabled,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
} 