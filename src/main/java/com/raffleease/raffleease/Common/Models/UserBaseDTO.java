package com.raffleease.raffleease.Common.Models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;

import static com.raffleease.raffleease.Common.Utils.SanitizeUtils.trimAndLower;

@Builder
@Getter
public class UserBaseDTO {
    @NotBlank
    @Size(min = 2, max = 50)
    private final String firstName;

    @NotBlank
    @Size(min = 2, max = 50)
    private final String lastName;

    @NotBlank
    @Size(min = 2, max = 25)
    private final String userName;

    @JsonCreator
    public UserBaseDTO(
            @JsonProperty("firstName") String firstName,
            @JsonProperty("lastName") String lastName,
            @JsonProperty("userName") String userName
    ) {
        this.firstName = trimAndLower(firstName);
        this.lastName = trimAndLower(lastName);
        this.userName = trimAndLower(userName);
    }
}
