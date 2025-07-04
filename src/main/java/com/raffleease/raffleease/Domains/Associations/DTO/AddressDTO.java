package com.raffleease.raffleease.Domains.Associations.DTO;

import lombok.Builder;

@Builder
public record AddressDTO(
        String placeId,
        Double latitude,
        Double longitude,
        String city,
        String province,
        String zipCode,
        String formattedAddress
) {}