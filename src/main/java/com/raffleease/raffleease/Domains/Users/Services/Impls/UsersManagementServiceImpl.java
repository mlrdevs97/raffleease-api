package com.raffleease.raffleease.Domains.Users.Services.Impls;

import com.raffleease.raffleease.Common.Configs.CorsProperties;
import com.raffleease.raffleease.Common.Exceptions.CustomExceptions.*;
import com.raffleease.raffleease.Common.Models.UserBaseDTO;
import com.raffleease.raffleease.Domains.Associations.Model.Association;
import com.raffleease.raffleease.Domains.Associations.Model.AssociationRole;
import com.raffleease.raffleease.Domains.Associations.Services.AssociationsMembershipService;
import com.raffleease.raffleease.Domains.Associations.Services.AssociationsService;
import com.raffleease.raffleease.Domains.Auth.DTOs.EditPasswordRequest;
import com.raffleease.raffleease.Domains.Auth.Model.VerificationToken;
import com.raffleease.raffleease.Domains.Auth.Repository.VerificationTokenRepository;
import com.raffleease.raffleease.Domains.Notifications.Services.EmailsService;
import com.raffleease.raffleease.Domains.Users.DTOs.CreateUserRequest;
import com.raffleease.raffleease.Domains.Users.DTOs.UpdatePhoneNumberRequest;
import com.raffleease.raffleease.Domains.Users.DTOs.UpdateUserRoleRequest;
import com.raffleease.raffleease.Domains.Users.DTOs.UserResponse;
import com.raffleease.raffleease.Domains.Users.Model.User;
import com.raffleease.raffleease.Domains.Users.Model.UserPhoneNumber;
import com.raffleease.raffleease.Domains.Users.Services.UsersManagementService;
import com.raffleease.raffleease.Domains.Users.Services.UsersService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDateTime;
import java.util.UUID;

import static com.raffleease.raffleease.Common.Constants.Constants.EMAIL_VERIFICATION_EXPIRATION_MINUTES;
import static com.raffleease.raffleease.Domains.Associations.Model.AssociationRole.ADMIN;
import static com.raffleease.raffleease.Common.Exceptions.ErrorCodes.CURRENT_PASSWORD_INCORRECT;
import static com.raffleease.raffleease.Common.Exceptions.ErrorCodes.PASSWORD_SAME_AS_CURRENT;
import static com.raffleease.raffleease.Common.Exceptions.ErrorCodes.ROLE_UPDATE_SELF_DENIED;
import static com.raffleease.raffleease.Common.Exceptions.ErrorCodes.ROLE_UPDATE_ADMIN_DENIED;
import static com.raffleease.raffleease.Common.Exceptions.ErrorCodes.ADMIN_DISABLE_SELF_DENIED;
import static com.raffleease.raffleease.Common.Exceptions.ErrorCodes.ADMIN_CREATE_ADMIN_DENIED;

@Slf4j
@RequiredArgsConstructor
@Service
public class UsersManagementServiceImpl implements UsersManagementService {
    private final UsersService usersService;
    private final AssociationsService associationsService;
    private final AssociationsMembershipService membershipService;
    private final PasswordEncoder passwordEncoder;
    private final VerificationTokenRepository verificationTokenRepository;
    private final EmailsService emailsService;
    private final CorsProperties corsProperties;

    @Transactional
    @Override
    public UserResponse create(Long associationId, CreateUserRequest request) {
        if (request.role() == ADMIN) {
            throw new BusinessException("Administrators cannot create other administrator accounts", ADMIN_CREATE_ADMIN_DENIED);
        }
        String encodedPassword = passwordEncoder.encode(request.userData().getPassword());
        User user = usersService.createUser(request.userData(), encodedPassword, false);
        Association association = associationsService.findById(associationId);
        associationsService.createAssociationMembership(association, user, request.role());
        handleUserVerification(user, association.getName());
        return usersService.getUserResponseById(user.getId());
    }

    @Transactional
    @Override
    public UserResponse edit(Long userId, UserBaseDTO userData) {
        User user = usersService.findById(userId);
        User updatedUser = usersService.updateUser(user, userData);
        return usersService.getUserResponseById(updatedUser.getId());
    }

    @Transactional
    @Override
    public void disableUserInAssociation(Long associationId, Long userId) {
        Association association = associationsService.findById(associationId);
        User user = usersService.findById(userId);
        User authenticatedUser = usersService.getAuthenticatedUser();
        if (authenticatedUser.getId().equals(userId)) {
            throw new BusinessException("Administrators cannot disable their own account", ADMIN_DISABLE_SELF_DENIED);
        }
        validateMembership(association, user);
        usersService.setUserEnabled(user, false);
    }

    @Transactional
    @Override
    public void enableUserInAssociation(Long associationId, Long userId) {
        Association association = associationsService.findById(associationId);
        User user = usersService.findById(userId);
        validateMembership(association, user);
        usersService.setUserEnabled(user, true);
    }

    @Transactional
    @Override
    public void editPassword(EditPasswordRequest request) {
        User authenticatedUser = usersService.getAuthenticatedUser();

        if (!passwordEncoder.matches(request.currentPassword(), authenticatedUser.getPassword())) {
            throw new PasswordResetException("Current password is incorrect", CURRENT_PASSWORD_INCORRECT);
        }

        if (passwordEncoder.matches(request.password(), authenticatedUser.getPassword())) {
            throw new BusinessException("New password must be different from current password", PASSWORD_SAME_AS_CURRENT);
        }

        String encodedNewPassword = passwordEncoder.encode(request.password());
        usersService.updatePassword(authenticatedUser, encodedNewPassword);
    }

    @Transactional
    @Override
    public UserResponse updatePhoneNumber(Long associationId, Long userId, UpdatePhoneNumberRequest request) {
        Association association = associationsService.findById(associationId);
        User user = usersService.findById(userId);
        validateMembership(association, user);
        UserPhoneNumber phoneNumber = UserPhoneNumber.builder()
            .prefix(request.phoneNumber().prefix())
            .nationalNumber(request.phoneNumber().nationalNumber())
            .build();
        user.setPhoneNumber(phoneNumber);
        User updatedUser = usersService.save(user);
        return usersService.getUserResponseById(updatedUser.getId());
    }

    @Transactional
    @Override
    public UserResponse updateUserRole(Long associationId, Long userId, UpdateUserRoleRequest request) {
        Association association = associationsService.findById(associationId);
        User user = usersService.findById(userId);
        validateMembership(association, user);
        User currentUser = usersService.getAuthenticatedUser();

        if (currentUser.getId().equals(userId)) {
            throw new UpdateRoleException("Administrators cannot update their own role", ROLE_UPDATE_SELF_DENIED);
        }

        AssociationRole currentRole = membershipService.getUserRoleInAssociation(user);
        if (currentRole == ADMIN) {
            throw new UpdateRoleException("Administrator roles cannot be updated", ROLE_UPDATE_ADMIN_DENIED);
        }

        membershipService.updateUserRole(user, request.role());
        return usersService.getUserResponseById(userId);
    }

    private void validateMembership(Association association, User user) {
        try {
            membershipService.validateIsMember(association, user);
        } catch (Exception ex) {
            if (ex instanceof AuthorizationException) {
                throw new BusinessException(ex.getMessage());
            }
            throw ex;
        }
    }

    private void handleUserVerification(User user, String associationName) {
        VerificationToken verificationToken = createVerificationToken(user);
        String verificationLink = UriComponentsBuilder.fromHttpUrl(corsProperties.getClientAsList().get(0))
                .path("/auth/email-verification")
                .queryParam("token", verificationToken.getToken())
                .build()
                .toUriString();
        emailsService.sendUserCreationVerificationEmail(user, associationName, verificationLink);
    }

    private VerificationToken createVerificationToken(User user) {
        try {
            return verificationTokenRepository.save(VerificationToken.builder()
                    .token(UUID.randomUUID().toString())
                    .user(user)
                    .expiryDate(LocalDateTime.now().plusMinutes(EMAIL_VERIFICATION_EXPIRATION_MINUTES))
                    .build());
        } catch (DataAccessException ex) {
            throw new DatabaseException("Database error occurred while saving verification token");
        }
    }
} 