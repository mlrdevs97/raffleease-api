package com.raffleease.raffleease.Domains.Auth.Controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.raffleease.raffleease.Base.AbstractIntegrationTest;
import com.raffleease.raffleease.Domains.Associations.Model.AssociationRole;
import com.raffleease.raffleease.Domains.Auth.DTOs.ForgotPasswordRequest;
import com.raffleease.raffleease.Domains.Auth.DTOs.ResetPasswordRequest;
import com.raffleease.raffleease.Domains.Auth.Model.PasswordResetToken;
import com.raffleease.raffleease.Domains.Auth.Repository.PasswordResetTokenRepository;
import com.raffleease.raffleease.Domains.Notifications.Services.EmailsService;
import com.raffleease.raffleease.Domains.Users.Model.User;
import com.raffleease.raffleease.Domains.Users.Repository.UsersRepository;
import com.raffleease.raffleease.util.AuthTestUtils;
import com.raffleease.raffleease.util.AuthTestUtils.AuthTestData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("Password Reset Controller Integration Tests")
class PasswordResetControllerIT extends AbstractIntegrationTest {
    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AuthTestUtils authTestUtils;

    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Autowired
    private UsersRepository usersRepository;

    @MockitoBean
    private EmailsService emailsService;

    private static final String FORGOT_PASSWORD_ENDPOINT = "/v1/auth/forgot-password";
    private static final String RESET_PASSWORD_ENDPOINT = "/v1/auth/reset-password";

    @Nested
    @DisplayName("POST /v1/auth/forgot-password - Request Password Reset")
    class ForgotPasswordTests {

        @Test
        @DisplayName("Should successfully send password reset email for existing user")
        void shouldSendPasswordResetEmailForExistingUser() throws Exception {
            // Arrange
            AuthTestData userData = authTestUtils.createAuthenticatedUser(true, AssociationRole.ADMIN);
            ForgotPasswordRequest request = new ForgotPasswordRequest(userData.user().getEmail());

            // Act
            ResultActions result = mockMvc.perform(post(FORGOT_PASSWORD_ENDPOINT)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)));

            // Assert
            result.andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Password reset link has been sent successfully"))
                    .andExpect(jsonPath("$.data").isEmpty());

            // Verify password reset token was created
            var tokenOptional = passwordResetTokenRepository.findByUser(userData.user());
            assertThat(tokenOptional).isPresent();
            assertThat(tokenOptional.get().getExpiryDate()).isAfter(LocalDateTime.now());
        }

        @Test
        @DisplayName("Should return success for non-existent email without revealing existence")
        void shouldReturnSuccessForNonExistentEmail() throws Exception {
            // Arrange
            ForgotPasswordRequest request = new ForgotPasswordRequest("nonexistent@example.com");

            // Act
            ResultActions result = mockMvc.perform(post(FORGOT_PASSWORD_ENDPOINT)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)));

            // Assert - Same response as existing user for security
            result.andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Password reset link has been sent successfully"))
                    .andExpect(jsonPath("$.data").isEmpty());
        }

        @Test
        @DisplayName("Should return 400 when email is invalid")
        void shouldReturn400WhenEmailIsInvalid() throws Exception {
            // Arrange
            ForgotPasswordRequest request = new ForgotPasswordRequest("invalid-email");

            // Act
            ResultActions result = mockMvc.perform(post(FORGOT_PASSWORD_ENDPOINT)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)));

            // Assert
            result.andExpect(status().isBadRequest())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("Validation failed"));
        }

        @Test
        @DisplayName("Should return 400 when email is blank")
        void shouldReturn400WhenEmailIsBlank() throws Exception {
            // Arrange
            ForgotPasswordRequest request = new ForgotPasswordRequest("");

            // Act
            ResultActions result = mockMvc.perform(post(FORGOT_PASSWORD_ENDPOINT)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)));

            // Assert
            result.andExpect(status().isBadRequest())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("Validation failed"));
        }

        @Test
        @DisplayName("Should replace existing token when user requests password reset again")
        void shouldReplaceExistingTokenWhenUserRequestsAgain() throws Exception {
            // Arrange
            AuthTestData userData = authTestUtils.createAuthenticatedUser(true, AssociationRole.ADMIN);
            User user = userData.user();
            
            // Create existing token
            PasswordResetToken existingToken = PasswordResetToken.builder()
                    .token("existing-token")
                    .user(user)
                    .expiryDate(LocalDateTime.now().plusHours(1))
                    .build();
            passwordResetTokenRepository.save(existingToken);

            ForgotPasswordRequest request = new ForgotPasswordRequest(user.getEmail());

            // Act
            ResultActions result = mockMvc.perform(post(FORGOT_PASSWORD_ENDPOINT)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)));

            // Assert
            result.andExpect(status().isOk());

            // Verify old token was replaced
            var tokenOptional = passwordResetTokenRepository.findByUser(user);
            assertThat(tokenOptional).isPresent();
            assertThat(tokenOptional.get().getToken()).isNotEqualTo("existing-token");
        }
    }

    @Nested
    @DisplayName("POST /v1/auth/reset-password - Reset Password")
    class ResetPasswordTests {

        @Test
        @DisplayName("Should successfully reset password with valid token")
        void shouldResetPasswordWithValidToken() throws Exception {
            // Arrange
            AuthTestData userData = authTestUtils.createAuthenticatedUser(true, AssociationRole.ADMIN);
            User user = userData.user();
            String originalPassword = user.getPassword();
            
            // Create password reset token
            PasswordResetToken token = PasswordResetToken.builder()
                    .token(UUID.randomUUID().toString())
                    .user(user)
                    .expiryDate(LocalDateTime.now().plusHours(1))
                    .build();
            passwordResetTokenRepository.save(token);

            ResetPasswordRequest request = new ResetPasswordRequest(
                    token.getToken(),
                    "NewSecure#123",
                    "NewSecure#123"
            );

            // Act
            ResultActions result = mockMvc.perform(post(RESET_PASSWORD_ENDPOINT)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)));

            // Assert
            result.andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Password has been reset successfully"))
                    .andExpect(jsonPath("$.data").isEmpty());

            // Verify token was deleted
            var tokenOptional = passwordResetTokenRepository.findByToken(token.getToken());
            assertThat(tokenOptional).isEmpty();

            // Verify password was changed (refresh user from database)
            User updatedUser = usersRepository.findById(user.getId()).orElseThrow();
            assertThat(updatedUser.getPassword()).isNotEqualTo(originalPassword);
        }

        @Test
        @DisplayName("Should return 400 when token is invalid")
        void shouldReturn400WhenTokenIsInvalid() throws Exception {
            // Arrange
            ResetPasswordRequest request = new ResetPasswordRequest(
                    "invalid-token",
                    "NewSecure#123",
                    "NewSecure#123"
            );

            // Act
            ResultActions result = mockMvc.perform(post(RESET_PASSWORD_ENDPOINT)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)));

            // Assert
            result.andExpect(status().isBadRequest())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value(containsString("Invalid or expired password reset token")));
        }

        @Test
        @DisplayName("Should return 400 when token is expired")
        void shouldReturn400WhenTokenIsExpired() throws Exception {
            // Arrange
            AuthTestData userData = authTestUtils.createAuthenticatedUser(true, AssociationRole.ADMIN);
            User user = userData.user();
            
            // Create expired token
            PasswordResetToken expiredToken = PasswordResetToken.builder()
                    .token(UUID.randomUUID().toString())
                    .user(user)
                    .expiryDate(LocalDateTime.now().minusHours(1)) // Expired
                    .build();
            passwordResetTokenRepository.save(expiredToken);

            ResetPasswordRequest request = new ResetPasswordRequest(
                    expiredToken.getToken(),
                    "NewSecure#123",
                    "NewSecure#123"
            );

            // Act
            ResultActions result = mockMvc.perform(post(RESET_PASSWORD_ENDPOINT)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)));

            // Assert
            result.andExpect(status().isBadRequest())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value(containsString("expired")));

            // Verify expired token was deleted
            var tokenOptional = passwordResetTokenRepository.findByToken(expiredToken.getToken());
            assertThat(tokenOptional).isEmpty();
        }

        @Test
        @DisplayName("Should return 400 when passwords don't match")
        void shouldReturn400WhenPasswordsDontMatch() throws Exception {
            // Arrange
            AuthTestData userData = authTestUtils.createAuthenticatedUser(true, AssociationRole.ADMIN);
            User user = userData.user();
            
            PasswordResetToken token = PasswordResetToken.builder()
                    .token(UUID.randomUUID().toString())
                    .user(user)
                    .expiryDate(LocalDateTime.now().plusHours(1))
                    .build();
            passwordResetTokenRepository.save(token);

            ResetPasswordRequest request = new ResetPasswordRequest(
                    token.getToken(),
                    "NewSecure#123",
                    "DifferentPass#456" // Different password
            );

            // Act
            ResultActions result = mockMvc.perform(post(RESET_PASSWORD_ENDPOINT)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)));

            // Assert
            result.andExpect(status().isBadRequest())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("Validation failed"))
                    .andExpect(jsonPath("$.errors.confirmPassword").value("INVALID_FIELD"));
        }

        @Test
        @DisplayName("Should return 400 when password is weak")
        void shouldReturn400WhenPasswordIsWeak() throws Exception {
            // Arrange
            AuthTestData userData = authTestUtils.createAuthenticatedUser(true, AssociationRole.ADMIN);
            User user = userData.user();
            
            PasswordResetToken token = PasswordResetToken.builder()
                    .token(UUID.randomUUID().toString())
                    .user(user)
                    .expiryDate(LocalDateTime.now().plusHours(1))
                    .build();
            passwordResetTokenRepository.save(token);

            ResetPasswordRequest request = new ResetPasswordRequest(
                    token.getToken(),
                    "weak", // Weak password
                    "weak"
            );

            // Act
            ResultActions result = mockMvc.perform(post(RESET_PASSWORD_ENDPOINT)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)));

            // Assert
            result.andExpect(status().isBadRequest())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("Validation failed"))
                    .andExpect(jsonPath("$.errors.password").value("INVALID_FORMAT"));
        }
    }
} 