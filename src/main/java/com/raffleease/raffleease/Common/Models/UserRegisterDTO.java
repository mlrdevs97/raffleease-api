package com.raffleease.raffleease.Common.Models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.raffleease.raffleease.Common.Validations.PasswordMatches;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.*;
import lombok.Builder;
import lombok.Getter;

import static com.raffleease.raffleease.Common.Constants.ValidationPatterns.PASSWORD_PATTERN;
import static com.raffleease.raffleease.Common.Utils.SanitizeUtils.trim;

@Getter
@PasswordMatches
public class UserRegisterDTO extends UserProfileDTO {
    @NotBlank
    @Pattern(regexp = PASSWORD_PATTERN)
    private final String password;

    @NotBlank
    private final String confirmPassword;

    @JsonCreator
    public UserRegisterDTO(
            @JsonProperty("firstName") String firstName,
            @JsonProperty("lastName") String lastName,
            @JsonProperty("userName") String userName,
            @JsonProperty("email") String email,
            @JsonProperty("phoneNumber") PhoneNumberDTO phoneNumber,
            @JsonProperty("password") String password,
            @JsonProperty("confirmPassword") String confirmPassword
    ) {
        super(firstName, lastName, userName, email, phoneNumber);
        this.password = trim(password);
        this.confirmPassword = trim(confirmPassword);
    }
}