package com.raffleease.raffleease.Domains.Associations.DTO;

import lombok.Builder;

@Builder
public record AssociationDTO (
        Long id,
        String associationName,
        String description,
        String email,
        String phoneNumber,
        AddressDTO addressData
) {}