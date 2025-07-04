package com.raffleease.raffleease.Domains.Users.DTOs;

import com.raffleease.raffleease.Common.Models.PhoneNumberDTO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record UpdatePhoneNumberRequest(
        @NotNull
        @Valid
        PhoneNumberDTO phoneNumber
) {
} 