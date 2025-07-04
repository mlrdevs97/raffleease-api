package com.raffleease.raffleease.Domains.Users.Mappers;

import com.raffleease.raffleease.Common.Models.UserRegisterDTO;
import com.raffleease.raffleease.Common.Models.PhoneNumberDTO;
import com.raffleease.raffleease.Domains.Associations.Model.AssociationRole;
import com.raffleease.raffleease.Domains.Users.DTOs.UserResponse;
import com.raffleease.raffleease.Domains.Users.Model.UserPhoneNumber;
import com.raffleease.raffleease.Domains.Users.Model.User;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class UsersMapper {
    public User buildUser(UserRegisterDTO userData, String encodedPassword, boolean isEnabled) {
        UserPhoneNumber phoneNumber = new UserPhoneNumber();
        phoneNumber.setPrefix(userData.getPhoneNumber().prefix());
        phoneNumber.setNationalNumber(userData.getPhoneNumber().nationalNumber());

        return User.builder()
                .firstName(userData.getFirstName())
                .lastName(userData.getLastName())
                .userName(userData.getUserName())
                .email(userData.getEmail())
                .phoneNumber(phoneNumber)
                .password(encodedPassword)
                .isEnabled(isEnabled)
                .build();
    }

    public UserResponse toUserResponse(User user, AssociationRole role) {
        PhoneNumberDTO phoneNumber = PhoneNumberDTO.builder()
                .prefix(user.getPhoneNumber().getPrefix())
                .nationalNumber(user.getPhoneNumber().getNationalNumber())
                .build();

        return UserResponse.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .userName(user.getUserName())
                .email(user.getEmail())
                .phoneNumber(phoneNumber)
                .role(role)
                .isEnabled(user.isEnabled())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}
