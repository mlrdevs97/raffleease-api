package com.raffleease.raffleease.Domains.Users.Services;

import com.raffleease.raffleease.Common.Exceptions.CustomExceptions.AuthenticationException;
import com.raffleease.raffleease.Common.Models.UserBaseDTO;
import com.raffleease.raffleease.Common.Models.UserProfileDTO;
import com.raffleease.raffleease.Domains.Auth.DTOs.EditPasswordRequest;
import com.raffleease.raffleease.Domains.Users.DTOs.CreateUserRequest;
import com.raffleease.raffleease.Domains.Users.DTOs.UpdatePhoneNumberRequest;
import com.raffleease.raffleease.Domains.Users.DTOs.UpdateUserRoleRequest;
import com.raffleease.raffleease.Domains.Users.DTOs.UserResponse;

public interface UsersManagementService {
    /**
     * Create a new user account.
     * Only ADMINS of the association can create new user accounts for the association.
     *
     * @param associationId The ID of the association
     * @param request The request containing user data and role
     * @return The created user response
     */
    UserResponse create(Long associationId, CreateUserRequest request);

    /**
     * Edit a user account.
     * AN user can only edit their own account.
     * Only ADMINS of the association can edit other user accounts.
     *
     * @param userId The ID of the user to edit
     * @param userData The data to edit the user with
     * @return The updated user response
     */
    UserResponse edit(Long userId, UserBaseDTO userData);

    /**
     * Disable a user account. Disabled accounts cannot login to the application.
     * Only ADMINS of the association can disable user accounts.
     * ADMIN account cannot be disabled.
     * 
     * @param associationId The ID of the association
     * @param userId The ID of the user to disable
     */
    void disableUserInAssociation(Long associationId, Long userId);
    
    /**
     * Enable a user account. Enabled accounts can login to the application.
     * Only ADMINS of the association can enable user accounts.
     *
     * @param associationId The ID of the association
     * @param userId The ID of the user to enable
     */
    void enableUserInAssociation(Long associationId, Long userId);

    /**
     * Edit password for an authenticated user using their current password.
     * Only authenticated users can edit their own password.
     * ADMIN cannot edit other users' passwords.
     *
     * @param request The request containing current password, new password, and confirmation
     * @throws AuthenticationException If the current password is incorrect
     */
    void editPassword(EditPasswordRequest request);

    /**
     * Update the phone number for a user account.
     * Only the user can update their own phone number.
     *
     * @param associationId The ID of the association
     * @param userId The ID of the user to update
     * @param request The request containing the new phone number
     * @return The updated user response
     */
    UserResponse updatePhoneNumber(Long associationId, Long userId, UpdatePhoneNumberRequest request);

    /**
     * Update the role of a user in the association.
     * Only ADMINS of the association can update user roles.
     * The role can only be updated to MEMBER or COLLABORATOR, not ADMIN.
     * Administrators cannot update their own role.
     *
     * @param associationId The ID of the association
     * @param userId The ID of the user to update
     * @param request The request containing the new role
     * @return The updated user response
     */
    UserResponse updateUserRole(Long associationId, Long userId, UpdateUserRoleRequest request);
} 