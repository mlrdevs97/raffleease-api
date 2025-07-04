package com.raffleease.raffleease.Domains.Users.DTOs;

import com.raffleease.raffleease.Common.Models.UserBaseDTO;
import com.raffleease.raffleease.Common.Models.UserProfileDTO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record EditUserRequest(
        @NotNull
        @Valid
        UserBaseDTO userData
) { }
