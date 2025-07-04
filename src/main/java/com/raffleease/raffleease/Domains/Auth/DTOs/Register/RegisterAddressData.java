package com.raffleease.raffleease.Domains.Auth.DTOs.Register;

import jakarta.validation.constraints.*;
import lombok.Builder;

@Builder
public record RegisterAddressData(
        @NotBlank
        String placeId,

        @NotNull
        @DecimalMin(value = "-90.0")
        @DecimalMax(value = "90.0")
        Double latitude,

        @NotNull
        @DecimalMin(value = "-180.0")
        @DecimalMax(value = "180.0")
        Double longitude,

        @NotBlank
        @Size(min = 2, max = 100)
        String city,

        @Size(min = 2, max = 100)
        String province,

        @Pattern(regexp = "^$|^[0-9]{5}(?:-[0-9]{4})?$")
        String zipCode,

        @NotBlank
        @Size(min = 5, max = 255)
        String formattedAddress
) {}