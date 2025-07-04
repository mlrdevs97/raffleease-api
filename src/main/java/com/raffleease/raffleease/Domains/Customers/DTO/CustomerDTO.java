package com.raffleease.raffleease.Domains.Customers.DTO;

import com.raffleease.raffleease.Common.Models.PhoneNumberDTO;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record CustomerDTO(
        Long id,
        String fullName,
        String email,
        PhoneNumberDTO phoneNumber,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}