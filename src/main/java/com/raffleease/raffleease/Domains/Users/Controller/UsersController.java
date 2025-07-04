package com.raffleease.raffleease.Domains.Users.Controller;

import com.raffleease.raffleease.Common.Responses.ApiResponse;
import com.raffleease.raffleease.Common.Responses.ResponseFactory;
import com.raffleease.raffleease.Domains.Auth.DTOs.EditPasswordRequest;
import com.raffleease.raffleease.Domains.Auth.Validations.ValidateAssociationAccess;
import com.raffleease.raffleease.Domains.Auth.Validations.AdminOnly;
import com.raffleease.raffleease.Domains.Auth.Validations.RequireRole;
import com.raffleease.raffleease.Domains.Auth.Validations.SelfAccessOnly;
import com.raffleease.raffleease.Domains.Users.DTOs.CreateUserRequest;
import com.raffleease.raffleease.Domains.Users.DTOs.EditUserRequest;
import com.raffleease.raffleease.Domains.Users.DTOs.UpdateEmailRequest;
import com.raffleease.raffleease.Domains.Users.DTOs.UpdatePhoneNumberRequest;
import com.raffleease.raffleease.Domains.Users.DTOs.UpdateUserRoleRequest;
import com.raffleease.raffleease.Domains.Users.DTOs.UserSearchFilters;
import com.raffleease.raffleease.Domains.Users.DTOs.VerifyEmailUpdateRequest;
import com.raffleease.raffleease.Domains.Users.DTOs.UserResponse;
import com.raffleease.raffleease.Domains.Users.Services.UsersManagementService;
import com.raffleease.raffleease.Domains.Users.Services.UsersService;
import com.raffleease.raffleease.Domains.Users.Services.EmailUpdateService;
import com.raffleease.raffleease.Common.RateLimiting.RateLimit;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import static com.raffleease.raffleease.Common.RateLimiting.RateLimit.AccessLevel.PRIVATE;
import static com.raffleease.raffleease.Domains.Associations.Model.AssociationRole.ADMIN;
import static org.springframework.http.HttpStatus.CREATED;

@RequiredArgsConstructor
@RequestMapping("/v1/associations/{associationId}/users")
@RestController
@ValidateAssociationAccess
public class UsersController {
    private final UsersManagementService usersManagementService;
    private final UsersService usersService;
    private final EmailUpdateService emailUpdateService;

    @PostMapping
    @AdminOnly(message = "Only administrators can create user accounts")
    @RateLimit(operation = "create", accessLevel = PRIVATE)
    public ResponseEntity<ApiResponse> create(
            @PathVariable Long associationId,
            @Valid @RequestBody CreateUserRequest request
    ) {
        return ResponseEntity.status(CREATED).body(
                ResponseFactory.success(
                        usersManagementService.create(associationId, request),
                        "User account created successfully. Verification email sent."
                )
        );
    }

    @GetMapping
    @AdminOnly(message = "Only administrators can access user accounts information")
    @RateLimit(operation = "read", accessLevel = PRIVATE)
    public ResponseEntity<ApiResponse> search(
            @PathVariable Long associationId,
            UserSearchFilters searchFilters,
            Pageable pageable
    ) {
        return ResponseEntity.ok(
                ResponseFactory.success(
                        usersService.search(associationId, searchFilters, pageable),
                        "Users retrieved successfully"
                )
        );
    }

    @GetMapping("/{userId}")
    @RequireRole(
        value = ADMIN,
        allowSelfAccess = true,
        message = "Only administrators can access other users' account information, or users can access their own account"
    )
    @RateLimit(operation = "read", accessLevel = PRIVATE)
    public ResponseEntity<ApiResponse> get(
            @PathVariable Long associationId,
            @PathVariable Long userId
    ) {
        UserResponse user = usersService.getUserResponseById(userId);
        return ResponseEntity.ok().body(
                ResponseFactory.success(
                        user,
                        "User retrieved successfully"
                )
        );
    }

    @PutMapping("/{userId}")
    @SelfAccessOnly(message = "You can only update your own account")
    @RateLimit(operation = "update", accessLevel = PRIVATE)
    public ResponseEntity<ApiResponse> edit(
            @PathVariable Long associationId,
            @PathVariable Long userId,
            @Valid @RequestBody EditUserRequest request
    ) {
        return ResponseEntity.ok().body(
                ResponseFactory.success(
                        usersManagementService.edit(userId, request.userData()),
                        "User updated successfully"
                )
        );
    }

    @PatchMapping("/{userId}/password")
    @SelfAccessOnly(message = "You can only change your own password")
    @RateLimit(operation = "update", accessLevel = PRIVATE)
    public ResponseEntity<ApiResponse> editPassword(
            @PathVariable Long associationId,
            @PathVariable Long userId,
            @Valid @RequestBody EditPasswordRequest request
    ) {
        usersManagementService.editPassword(request);
        return ResponseEntity.ok().body(
                ResponseFactory.success(
                        null,
                        "Password has been updated successfully"
                )
        );
    }

    @PatchMapping("/{userId}/disable")
    @AdminOnly(message = "Only administrators can disable user accounts")
    @RateLimit(operation = "delete", accessLevel = PRIVATE)
    public ResponseEntity<ApiResponse> disableUser(
            @PathVariable Long associationId,
            @PathVariable Long userId
    ) {
        usersManagementService.disableUserInAssociation(associationId, userId);
        return ResponseEntity.ok().body(
                ResponseFactory.success(
                        null,
                        "User disabled successfully"
                )
        );
    }

    @PatchMapping("/{userId}/enable")
    @AdminOnly(message = "Only administrators can enable user accounts")
    @RateLimit(operation = "delete", accessLevel = PRIVATE)
    public ResponseEntity<ApiResponse> enableUser(
            @PathVariable Long associationId,
            @PathVariable Long userId
    ) {
        usersManagementService.enableUserInAssociation(associationId, userId);
        return ResponseEntity.ok().body(
                ResponseFactory.success(
                        null,
                        "User enabled successfully"
                )
        );
    }

    @PostMapping("/{userId}/email/verification-request")
    @SelfAccessOnly(message = "You can only update your own email address")
    @RateLimit(operation = "update", accessLevel = PRIVATE)
    public ResponseEntity<ApiResponse> requestEmailUpdate(
            @PathVariable Long associationId,
            @PathVariable Long userId,
            @Valid @RequestBody UpdateEmailRequest request
    ) {
        emailUpdateService.requestEmailUpdate(userId, request);
        return ResponseEntity.ok().body(
                ResponseFactory.success(
                        null,
                        "Email update verification has been sent to your new email address"
                )
        );
    }

    @PostMapping("/verify-email-update")
    @RateLimit(operation = "update", accessLevel = PRIVATE)
    public ResponseEntity<ApiResponse> verifyEmailUpdate(
            @PathVariable Long associationId,
            @Valid @RequestBody VerifyEmailUpdateRequest request
    ) {
        emailUpdateService.verifyEmailUpdate(request);
        return ResponseEntity.ok().body(
                ResponseFactory.success(
                        null,
                        "Email has been updated successfully"
                )
        );
    }

    @PatchMapping("/{userId}/phone-number")
    @SelfAccessOnly(message = "You can only update your own phone number")
    @RateLimit(operation = "update", accessLevel = PRIVATE)
    public ResponseEntity<ApiResponse> updatePhoneNumber(
            @PathVariable Long associationId,
            @PathVariable Long userId,
            @Valid @RequestBody UpdatePhoneNumberRequest request
    ) {
        UserResponse updatedUser = usersManagementService.updatePhoneNumber(associationId, userId, request);
        return ResponseEntity.ok().body(
                ResponseFactory.success(
                        updatedUser,
                        "Phone number has been updated successfully"
                )
        );
    }

    @PatchMapping("/{userId}/role")
    @AdminOnly(message = "Only administrators can update user roles")
    @RateLimit(operation = "update", accessLevel = PRIVATE)
    public ResponseEntity<ApiResponse> updateUserRole(
            @PathVariable Long associationId,
            @PathVariable Long userId,
            @Valid @RequestBody UpdateUserRoleRequest request
    ) {
        UserResponse updatedUser = usersManagementService.updateUserRole(associationId, userId, request);
        return ResponseEntity.ok().body(
                ResponseFactory.success(
                        updatedUser,
                        "User role has been updated successfully"
                )
        );
    }
} 