package com.raffleease.raffleease.Domains.Auth.Controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.raffleease.raffleease.Base.AbstractIntegrationTest;
import com.raffleease.raffleease.Domains.Auth.DTOs.LoginRequest;
import com.raffleease.raffleease.util.AuthTestUtils;
import com.raffleease.raffleease.util.AuthTestUtils.AuthTestData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("Auth Controller Integration Tests")
class AuthControllerIT extends AbstractIntegrationTest {

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AuthTestUtils authTestUtils;

    private static final String LOGIN_ENDPOINT = "/v1/auth/login";

    @Nested
    @DisplayName("POST /v1/auth/login")
    class LoginEndpointTests {
        
        @Test
        @DisplayName("Should successfully authenticate user with valid username and password")
        void shouldAuthenticateUserWithValidUsernameAndPassword() throws Exception {
            // Arrange
            AuthTestData testData = authTestUtils.createAuthenticatedUser();
            LoginRequest loginRequest = new LoginRequest(testData.user().getUserName(), testData.plainTextPassword());

            // Act
            ResultActions result = mockMvc.perform(post(LOGIN_ENDPOINT)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(loginRequest)));

            // Assert
            result.andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("User authenticated successfully"))
                    .andExpect(jsonPath("$.data.accessToken").exists())
                    .andExpect(jsonPath("$.data.accessToken").isString())
                    .andExpect(jsonPath("$.data.associationId").value(testData.association().getId()))
                    .andExpect(cookie().exists("refresh_token"));
        }

        @Test
        @DisplayName("Should successfully authenticate user with valid email and password")
        void shouldAuthenticateUserWithValidEmailAndPassword() throws Exception {
            // Arrange
            AuthTestData testData = authTestUtils.createAuthenticatedUser();
            LoginRequest loginRequest = new LoginRequest(testData.user().getEmail(), testData.plainTextPassword());

            // Act
            ResultActions result = mockMvc.perform(post(LOGIN_ENDPOINT)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(loginRequest)));

            // Assert
            result.andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("User authenticated successfully"))
                    .andExpect(jsonPath("$.data.accessToken").exists())
                    .andExpect(jsonPath("$.data.associationId").value(testData.association().getId()))
                    .andExpect(cookie().exists("refresh_token"));
        }

        @Test
        @DisplayName("Should return 401 when user credentials are invalid")
        void shouldReturn401WhenCredentialsAreInvalid() throws Exception {
            // Arrange
            AuthTestData testData = authTestUtils.createAuthenticatedUser();
            LoginRequest loginRequest = new LoginRequest(testData.user().getUserName(), "wrongPassword");

            // Act
            ResultActions result = mockMvc.perform(post(LOGIN_ENDPOINT)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(loginRequest)));

            // Assert
            result.andExpect(status().isUnauthorized())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").exists());
        }

        @Test
        @DisplayName("Should return 401 when user does not exist")
        void shouldReturn401WhenUserDoesNotExist() throws Exception {
            // Arrange
            LoginRequest loginRequest = new LoginRequest("nonexistent@example.com", "anyPassword");

            // Act
            ResultActions result = mockMvc.perform(post(LOGIN_ENDPOINT)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(loginRequest)));

            // Assert
            result.andExpect(status().isUnauthorized())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").exists());
        }

        @Test
        @DisplayName("Should return 401 when user account is disabled")
        void shouldReturn401WhenUserAccountIsDisabled() throws Exception {
            // Arrange
            AuthTestData testData = authTestUtils.createDisabledUser();
            LoginRequest loginRequest = new LoginRequest(testData.user().getUserName(), testData.plainTextPassword());

            // Act
            ResultActions result = mockMvc.perform(post(LOGIN_ENDPOINT)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(loginRequest)));

            // Assert
            result.andExpect(status().isUnauthorized())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").exists());
        }

        @Test
        @DisplayName("Should return 400 when request body is invalid")
        void shouldReturn400WhenRequestBodyIsInvalid() throws Exception {
            // Arrange
            String invalidJson = "{\"identifier\": \"\", \"password\": \"\"}";

            // Act
            ResultActions result = mockMvc.perform(post(LOGIN_ENDPOINT)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(invalidJson));

            // Assert
            result.andExpect(status().isBadRequest())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON));
        }

        @Test
        @DisplayName("Should return 400 when request body is missing")
        void shouldReturn400WhenRequestBodyIsMissing() throws Exception {
            // Act
            ResultActions result = mockMvc.perform(post(LOGIN_ENDPOINT)
                    .contentType(MediaType.APPLICATION_JSON));

            // Assert
            result.andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 415 when content type is not JSON")
        void shouldReturn415WhenContentTypeIsNotJson() throws Exception {
            // Arrange
            AuthTestData testData = authTestUtils.createAuthenticatedUser();
            LoginRequest loginRequest = new LoginRequest("test@example.com", testData.plainTextPassword());

            // Act
            ResultActions result = mockMvc.perform(post(LOGIN_ENDPOINT)
                    .contentType(MediaType.TEXT_PLAIN)
                    .content(objectMapper.writeValueAsString(loginRequest)));

            // Assert
            result.andExpect(status().isUnsupportedMediaType());
        }
    }
} 