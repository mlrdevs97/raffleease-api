package com.raffleease.raffleease.Common.Models;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Builder;

import static com.raffleease.raffleease.Common.Utils.SanitizeUtils.trim;
import static com.raffleease.raffleease.Common.Constants.ValidationPatterns.*;

/**
 * Shared phone number model used across multiple domains.
 * Provides validation and sanitization for international phone numbers.
 */
@Builder
public record PhoneNumberDTO(
        @NotBlank
        @Pattern(regexp = PHONE_PREFIX_PATTERN)
        String prefix,

        @NotBlank
        @Pattern(regexp = PHONE_NATIONAL_NUMBER_PATTERN)
        String nationalNumber
) {
    public PhoneNumberDTO {
        prefix = trim(prefix);
        nationalNumber = trim(nationalNumber);
    }
}