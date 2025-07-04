package com.raffleease.raffleease.Domains.Auth.Controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.raffleease.raffleease.Base.AbstractIntegrationTest;
import com.raffleease.raffleease.Common.Models.PhoneNumberDTO;
import com.raffleease.raffleease.Common.Models.UserBaseDTO;
import com.raffleease.raffleease.Domains.Associations.Model.AssociationRole;
import com.raffleease.raffleease.Domains.Users.DTOs.CreateUserRequest;
import com.raffleease.raffleease.Domains.Users.DTOs.EditUserRequest;
import com.raffleease.raffleease.Common.Models.UserProfileDTO;
import com.raffleease.raffleease.Common.Models.UserRegisterDTO;
import com.raffleease.raffleease.util.AuthTestUtils;
import com.raffleease.raffleease.util.AuthTestUtils.AuthTestData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.ResultActions;

import static com.raffleease.raffleease.Domains.Associations.Model.AssociationRole.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("Authorization Integration Tests")
class AuthorizationControllerIT extends AbstractIntegrationTest {

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AuthTestUtils authTestUtils;

    private static final String USERS_BASE_ENDPOINT = "/v1/associations/{associationId}/users";

    @Nested
    @DisplayName("ADMIN Role Authorization Tests")
    class AdminRoleTests {

        @Test
        @DisplayName("ADMIN should be able to create user with MEMBER ROLE")
        void adminShouldBeAbleToCreateUserWithMemberRole() throws Exception {
            // Arrange
            AuthTestData adminData = authTestUtils.createAuthenticatedUser(true, ADMIN);
            CreateUserRequest request = createValidCreateUserRequest(MEMBER);

            // Act
            ResultActions result = mockMvc.perform(post(USERS_BASE_ENDPOINT, adminData.association().getId())
                    .with(user(adminData.user().getUserName()).roles("USER"))
                    .contentType(APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)));

            // Assert
            result.andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("ADMIN should be able to create user with COLLABORATOR ROLE")
        void adminShouldBeAbleToCreateUserWithCollaboratorRoleRole() throws Exception {
            // Arrange
            AuthTestData adminData = authTestUtils.createAuthenticatedUser(true, ADMIN);
            CreateUserRequest request = createValidCreateUserRequest(COLLABORATOR);

            // Act
            ResultActions result = mockMvc.perform(post(USERS_BASE_ENDPOINT, adminData.association().getId())
                    .with(user(adminData.user().getUserName()).roles("USER"))
                    .contentType(APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)));

            // Assert
            result.andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("ADMIN should not be able to create user with ADMIN ROLE")
        void adminShouldNotBeAbleToCreateUserWithAdminRole() throws Exception {
            // Arrange
            AuthTestData adminData = authTestUtils.createAuthenticatedUser(true, ADMIN);
            CreateUserRequest request = createValidCreateUserRequest(ADMIN);

            // Act
            ResultActions result = mockMvc.perform(post(USERS_BASE_ENDPOINT, adminData.association().getId())
                    .with(user(adminData.user().getUserName()).roles("USER"))
                    .contentType(APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)));

            // Assert
            result.andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("Administrators cannot create other administrator accounts"));
        }

        @Test
        @DisplayName("ADMIN should be able to get all users")
        void adminShouldBeAbleToGetAllUsers() throws Exception {
            // Arrange
            AuthTestData adminData = authTestUtils.createAuthenticatedUser(true, ADMIN);

            // Act
            ResultActions result = mockMvc.perform(get(USERS_BASE_ENDPOINT, adminData.association().getId())
                    .with(user(adminData.user().getUserName()).roles("USER")));

            // Assert
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("ADMIN should NOT be able to update other users")
        void adminShouldNotBeAbleToUpdateOtherUsers() throws Exception {
            // Arrange
            AuthTestData adminData = authTestUtils.createAuthenticatedUser(true, ADMIN);
            AuthTestData memberData = authTestUtils.createAuthenticatedUserInSameAssociation(adminData.association());
            EditUserRequest request = createValidEditUserRequest();

            // Act
            ResultActions result = mockMvc.perform(put(USERS_BASE_ENDPOINT + "/{id}", 
                    adminData.association().getId(), memberData.user().getId())
                    .with(user(adminData.user().getUserName()).roles("USER"))
                    .contentType(APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)));

            // Assert
            result.andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("You can only update your own account"));
        }

        @Test
        @DisplayName("ADMIN should be able to disable other users")
        void adminShouldBeAbleToDisableOtherUsers() throws Exception {
            // Arrange
            AuthTestData adminData = authTestUtils.createAuthenticatedUser(true, ADMIN);
            AuthTestData memberData = authTestUtils.createAuthenticatedUserInSameAssociation(adminData.association());

            // Act
            ResultActions result = mockMvc.perform(patch(USERS_BASE_ENDPOINT + "/{userId}/disable", 
                    adminData.association().getId(), memberData.user().getId())
                    .with(user(adminData.user().getUserName()).roles("USER")));

            // Assert
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("ADMIN should NOT be able to disable themselves")
        void adminShouldNotBeAbleToDisableThemselves() throws Exception {
            // Arrange
            AuthTestData adminData = authTestUtils.createAuthenticatedUser(true, ADMIN);

            // Act
            ResultActions result = mockMvc.perform(patch(USERS_BASE_ENDPOINT + "/{userId}/disable", 
                    adminData.association().getId(), adminData.user().getId())
                    .with(user(adminData.user().getUserName()).roles("USER")));

            // Assert
            result.andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("Administrators cannot disable their own account"));
        }
    }

    @Nested
    @DisplayName("MEMBER Role Authorization Tests")
    class MemberRoleTests {

        @Test
        @DisplayName("MEMBER should NOT be able to create users")
        void memberShouldNotBeAbleToCreateUsers() throws Exception {
            // Arrange
            AuthTestData memberData = authTestUtils.createAuthenticatedUser(true, MEMBER);
            CreateUserRequest request = createValidCreateUserRequest(COLLABORATOR);

            // Act
            ResultActions result = mockMvc.perform(post(USERS_BASE_ENDPOINT, memberData.association().getId())
                    .with(user(memberData.user().getUserName()).roles("USER"))
                    .contentType(APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)));

            // Assert
            result.andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("Only administrators can create user accounts"));
        }

        @Test
        @DisplayName("MEMBER should NOT be able to get all users")
        void memberShouldNotBeAbleToGetAllUsers() throws Exception {
            // Arrange
            AuthTestData memberData = authTestUtils.createAuthenticatedUser(true, MEMBER);

            // Act
            ResultActions result = mockMvc.perform(get(USERS_BASE_ENDPOINT, memberData.association().getId())
                    .with(user(memberData.user().getUserName()).roles("USER")));

            // Assert
            result.andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("Only administrators can access user accounts information"));
        }

        @Test
        @DisplayName("MEMBER should be able to update their own account")
        void memberShouldBeAbleToUpdateOwnAccount() throws Exception {
            // Arrange
            AuthTestData memberData = authTestUtils.createAuthenticatedUser(true, MEMBER);
            EditUserRequest request = createValidEditUserRequest();

            // Act
            ResultActions result = mockMvc.perform(put(USERS_BASE_ENDPOINT + "/{id}", 
                    memberData.association().getId(), memberData.user().getId())
                    .with(user(memberData.user().getUserName()).roles("USER"))
                    .contentType(APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)));

            // Assert
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("MEMBER should NOT be able to update other users")
        void memberShouldNotBeAbleToUpdateOtherUsers() throws Exception {
            // Arrange
            AuthTestData memberData = authTestUtils.createAuthenticatedUser(true, MEMBER);
            AuthTestData otherMemberData = authTestUtils.createAuthenticatedUserInSameAssociation(memberData.association());
            EditUserRequest request = createValidEditUserRequest();

            // Act
            ResultActions result = mockMvc.perform(put(USERS_BASE_ENDPOINT + "/{id}", 
                    memberData.association().getId(), otherMemberData.user().getId())
                    .with(user(memberData.user().getUserName()).roles("USER"))
                    .contentType(APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)));

            // Assert
            result.andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("You can only update your own account"));
        }

        @Test
        @DisplayName("MEMBER should NOT be able to disable users")
        void memberShouldNotBeAbleToDisableUsers() throws Exception {
            // Arrange
            AuthTestData memberData = authTestUtils.createAuthenticatedUser(true, MEMBER);
            AuthTestData otherMemberData = authTestUtils.createAuthenticatedUserInSameAssociation(memberData.association());

            // Act
            ResultActions result = mockMvc.perform(patch(USERS_BASE_ENDPOINT + "/{userId}/disable", 
                    memberData.association().getId(), otherMemberData.user().getId())
                    .with(user(memberData.user().getUserName()).roles("USER")));

            // Assert
            result.andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("Only administrators can disable user accounts"));
        }
    }

    @Nested
    @DisplayName("COLLABORATOR Role Authorization Tests")
    class CollaboratorRoleTests {

        @Test
        @DisplayName("COLLABORATOR should NOT be able to create users")
        void collaboratorShouldNotBeAbleToCreateUsers() throws Exception {
            // Arrange
            AuthTestData collaboratorData = authTestUtils.createAuthenticatedUser(true, COLLABORATOR);
            CreateUserRequest request = createValidCreateUserRequest(COLLABORATOR);

            // Act
            ResultActions result = mockMvc.perform(post(USERS_BASE_ENDPOINT, collaboratorData.association().getId())
                    .with(user(collaboratorData.user().getUserName()).roles("USER"))
                    .contentType(APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)));

            // Assert
            result.andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("Only administrators can create user accounts"));
        }

        @Test
        @DisplayName("COLLABORATOR should be able to update their own account")
        void collaboratorShouldBeAbleToUpdateOwnAccount() throws Exception {
            // Arrange
            AuthTestData collaboratorData = authTestUtils.createAuthenticatedUser(true, COLLABORATOR);
            EditUserRequest request = createValidEditUserRequest();

            // Act
            ResultActions result = mockMvc.perform(put(USERS_BASE_ENDPOINT + "/{id}", 
                    collaboratorData.association().getId(), collaboratorData.user().getId())
                    .with(user(collaboratorData.user().getUserName()).roles("USER"))
                    .contentType(APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)));

            // Assert
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("COLLABORATOR should NOT be able to update other users")
        void collaboratorShouldNotBeAbleToUpdateOtherUsers() throws Exception {
            // Arrange
            AuthTestData collaboratorData = authTestUtils.createAuthenticatedUser(true, COLLABORATOR);
            AuthTestData memberData = authTestUtils.createAuthenticatedUserInSameAssociation(collaboratorData.association());
            EditUserRequest request = createValidEditUserRequest();

            // Act
            ResultActions result = mockMvc.perform(put(USERS_BASE_ENDPOINT + "/{id}", 
                    collaboratorData.association().getId(), memberData.user().getId())
                    .with(user(collaboratorData.user().getUserName()).roles("USER"))
                    .contentType(APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)));

            // Assert
            result.andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("You can only update your own account"));
        }
    }

    // Helper methods for creating test data
    private CreateUserRequest createValidCreateUserRequest(AssociationRole role) {
        PhoneNumberDTO phoneNumber = PhoneNumberDTO.builder()
                .prefix("+1")
                .nationalNumber("234567890")
                .build();

        UserRegisterDTO userData = new UserRegisterDTO(
                "John",
                "Doe", 
                "johndoe_new",
                "john.doe.new@example.com",
                phoneNumber,
                "SecurePass#123",
                "SecurePass#123"
        );

        return CreateUserRequest.builder()
                .userData(userData)
                .role(role)
                .build();
    }

    private EditUserRequest createValidEditUserRequest() {
        return new EditUserRequest(UserBaseDTO.builder()
                .firstName("Jane")
                .lastName("Smith")
                .userName("janesmith_updated")
                .build());
    }
} 