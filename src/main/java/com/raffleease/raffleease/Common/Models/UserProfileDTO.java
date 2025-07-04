package com.raffleease.raffleease.Common.Models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.annotation.Nullable;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import static com.raffleease.raffleease.Common.Utils.SanitizeUtils.trimAndLower;

@Getter
public class UserProfileDTO extends UserBaseDTO {
    @NotBlank
    @Email
    private final String email;

    @Valid
    @NotNull
    private final PhoneNumberDTO phoneNumber;

    @JsonCreator
    public UserProfileDTO(
            @JsonProperty("firstName") String firstName,
            @JsonProperty("lastName") String lastName,
            @JsonProperty("userName") String userName,
            @JsonProperty("email") String email,
            @JsonProperty("phoneNumber") PhoneNumberDTO phoneNumber
    ) {
        super(firstName, lastName, userName);
        this.email = trimAndLower(email);
        this.phoneNumber = phoneNumber;
    }
}