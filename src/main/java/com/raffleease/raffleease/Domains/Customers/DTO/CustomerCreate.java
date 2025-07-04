package com.raffleease.raffleease.Domains.Customers.DTO;

import com.raffleease.raffleease.Common.Models.PhoneNumberDTO;
import jakarta.annotation.Nullable;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

import static com.raffleease.raffleease.Common.Utils.SanitizeUtils.trimAndLower;

@Builder
public record CustomerCreate(
        @NotBlank
        String fullName,

        @Nullable
        @Email
        String email,

        @Nullable
        @Valid
        PhoneNumberDTO phoneNumber
) {
        public CustomerCreate {
                fullName = trimAndLower(fullName);
                email = trimAndLower(email);
        }
}

