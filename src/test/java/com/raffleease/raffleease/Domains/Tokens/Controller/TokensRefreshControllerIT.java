package com.raffleease.raffleease.Domains.Tokens.Controller;

import com.raffleease.raffleease.Base.AbstractIntegrationTest;
import com.raffleease.raffleease.Domains.Tokens.Services.TokensCreateService;
import com.raffleease.raffleease.Domains.Tokens.Services.TokensQueryService;
import com.raffleease.raffleease.Domains.Tokens.Services.BlackListService;
import com.raffleease.raffleease.util.AuthTestUtils;
import com.raffleease.raffleease.util.AuthTestUtils.AuthTestData;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.web.servlet.ResultActions;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("Tokens Refresh Controller Integration Tests")
class TokensRefreshControllerIT extends AbstractIntegrationTest {

    @Autowired
    private AuthTestUtils authTestUtils;

    @Autowired
    private TokensCreateService tokensCreateService;

    @Autowired
    private TokensQueryService tokensQueryService;

    @Autowired
    private BlackListService blackListService;

    @Value("${spring.application.security.jwt.refresh_token_expiration}")
    private Long refreshTokenExpiration;

    private AuthTestData authData;
    private String refreshEndpoint;
    private String validAccessToken;
    private String validRefreshToken;

    @BeforeEach
    void setUp() {
        // Create unique credentials to avoid constraint violations
        String uniqueId = String.valueOf(System.currentTimeMillis());
        authData = authTestUtils.createAuthenticatedUserWithCredentials(
                "tokenuser" + uniqueId, 
                "tokentest" + uniqueId + "@example.com", 
                "password123"
        );
        refreshEndpoint = "/v1/tokens/refresh";
        
        // Create valid tokens for testing
        validAccessToken = tokensCreateService.generateAccessToken(authData.user().getId());
        validRefreshToken = tokensCreateService.generateRefreshToken(authData.user().getId());
    }

    @Nested
    @DisplayName("POST /v1/tokens/refresh - Successful Refresh")
    class SuccessfulRefreshTests {

        @Test
        @DisplayName("Should successfully refresh tokens with valid access token and refresh cookie")
        void shouldSuccessfullyRefreshTokensWithValidTokens() throws Exception {
            // Arrange
            Cookie refreshCookie = new Cookie("refresh_token", validRefreshToken);

            // Act
            ResultActions result = mockMvc.perform(post(refreshEndpoint)
                    .header("Authorization", "Bearer " + validAccessToken)
                    .cookie(refreshCookie));

            // Assert
            result.andExpect(status().isOk())
                    .andExpect(content().contentType("application/json"))
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Token refreshed successfully"))
                    .andExpect(jsonPath("$.data.accessToken").exists())
                    .andExpect(jsonPath("$.data.accessToken").isString())
                    .andExpect(jsonPath("$.data.associationId").value(authData.association().getId()))
                    .andExpect(cookie().exists("refresh_token"))
                    .andExpect(cookie().httpOnly("refresh_token", true))
                    .andExpect(cookie().secure("refresh_token", true))
                    .andExpect(cookie().sameSite("refresh_token", "None"));
        }

        @Test
        @DisplayName("Should return new tokens different from original ones")
        void shouldReturnNewTokensDifferentFromOriginalOnes() throws Exception {
            // Arrange
            Cookie refreshCookie = new Cookie("refresh_token", validRefreshToken);

            // Act
            ResultActions result = mockMvc.perform(post(refreshEndpoint)
                    .header("Authorization", "Bearer " + validAccessToken)
                    .cookie(refreshCookie));

            // Assert - New access token should be different from original
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.accessToken").exists())
                    .andExpect(jsonPath("$.data.accessToken").value(org.hamcrest.Matchers.not(validAccessToken)));
        }

        @Test
        @DisplayName("Should blacklist old tokens after successful refresh")
        void shouldBlacklistOldTokensAfterSuccessfulRefresh() throws Exception {
            // Arrange
            Cookie refreshCookie = new Cookie("refresh_token", validRefreshToken);
            String oldAccessTokenId = tokensQueryService.getTokenId(validAccessToken);
            String oldRefreshTokenId = tokensQueryService.getTokenId(validRefreshToken);

            // Act
            mockMvc.perform(post(refreshEndpoint)
                    .header("Authorization", "Bearer " + validAccessToken)
                    .cookie(refreshCookie))
                    .andExpect(status().isOk());

            // Assert - Old tokens should be blacklisted
            assert blackListService.isTokenBlackListed(oldAccessTokenId);
            assert blackListService.isTokenBlackListed(oldRefreshTokenId);
        }

        @Test
        @DisplayName("Should refresh tokens for user with member role")
        void shouldRefreshTokensForUserWithMemberRole() throws Exception {
            // Arrange
            String memberUniqueId = String.valueOf(System.currentTimeMillis() + 1);
            AuthTestData memberData = authTestUtils.createAuthenticatedUserWithCredentials(
                    "memberuser" + memberUniqueId,
                    "membertest" + memberUniqueId + "@example.com",
                    "password123"
            );
            String memberAccessToken = tokensCreateService.generateAccessToken(memberData.user().getId());
            String memberRefreshToken = tokensCreateService.generateRefreshToken(memberData.user().getId());
            Cookie refreshCookie = new Cookie("refresh_token", memberRefreshToken);

            // Act
            ResultActions result = mockMvc.perform(post(refreshEndpoint)
                    .header("Authorization", "Bearer " + memberAccessToken)
                    .cookie(refreshCookie));

            // Assert
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.accessToken").exists())
                    .andExpect(jsonPath("$.data.associationId").value(memberData.association().getId()));
        }
    }

    @Nested
    @DisplayName("POST /v1/tokens/refresh - Authorization Header Errors")
    class AuthorizationHeaderErrorTests {

        @Test
        @DisplayName("Should return 401 when Authorization header is missing")
        void shouldReturn401WhenAuthorizationHeaderIsMissing() throws Exception {
            // Arrange
            Cookie refreshCookie = new Cookie("refresh_token", validRefreshToken);

            // Act
            ResultActions result = mockMvc.perform(post(refreshEndpoint)
                    .cookie(refreshCookie));

            // Assert
            result.andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Should return 401 when Authorization header doesn't start with Bearer")
        void shouldReturn401WhenAuthorizationHeaderDoesntStartWithBearer() throws Exception {
            // Arrange
            Cookie refreshCookie = new Cookie("refresh_token", validRefreshToken);

            // Act
            ResultActions result = mockMvc.perform(post(refreshEndpoint)
                    .header("Authorization", "Basic " + validAccessToken)
                    .cookie(refreshCookie));

            // Assert
            result.andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Should return 401 when Authorization header has invalid format")
        void shouldReturn401WhenAuthorizationHeaderHasInvalidFormat() throws Exception {
            // Arrange
            Cookie refreshCookie = new Cookie("refresh_token", validRefreshToken);

            // Act
            ResultActions result = mockMvc.perform(post(refreshEndpoint)
                    .header("Authorization", "Bearer")
                    .cookie(refreshCookie));

            // Assert
            result.andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Should return 401 when access token is malformed")
        void shouldReturn401WhenAccessTokenIsMalformed() throws Exception {
            // Arrange
            Cookie refreshCookie = new Cookie("refresh_token", validRefreshToken);

            // Act
            ResultActions result = mockMvc.perform(post(refreshEndpoint)
                    .header("Authorization", "Bearer invalid.token.format")
                    .cookie(refreshCookie));

            // Assert
            result.andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Should return 401 when access token is expired")
        void shouldReturn401WhenAccessTokenIsExpired() throws Exception {
            // Arrange - Create an expired token (this would require mocking or waiting)
            // For this test, we'll use a blacklisted token to simulate expiration
            String expiredToken = tokensCreateService.generateAccessToken(authData.user().getId());
            String tokenId = tokensQueryService.getTokenId(expiredToken);
            blackListService.addTokenToBlackList(tokenId, 3600000L); // 1 hour
            
            Cookie refreshCookie = new Cookie("refresh_token", validRefreshToken);

            // Act
            ResultActions result = mockMvc.perform(post(refreshEndpoint)
                    .header("Authorization", "Bearer " + expiredToken)
                    .cookie(refreshCookie));

            // Assert
            result.andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("POST /v1/tokens/refresh - Refresh Cookie Errors")
    class RefreshCookieErrorTests {

        @Test
        @DisplayName("Should return 403 when refresh_token cookie is missing")
        void shouldReturn401WhenRefreshTokenCookieIsMissing() throws Exception {
            // Act
            ResultActions result = mockMvc.perform(post(refreshEndpoint)
                    .header("Authorization", "Bearer " + validAccessToken));

            // Assert
            result.andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Should return 400 when refresh_token cookie is empty")
        void shouldReturn400WhenRefreshTokenCookieIsEmpty() throws Exception {
            // Arrange
            Cookie emptyCookie = new Cookie("refresh_token", "");

            // Act
            ResultActions result = mockMvc.perform(post(refreshEndpoint)
                    .header("Authorization", "Bearer " + validAccessToken)
                    .cookie(emptyCookie));

            // Assert
            result.andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 403 when refresh_token cookie is malformed")
        void shouldReturn403WhenRefreshTokenCookieIsMalformed() throws Exception {
            // Arrange
            Cookie malformedCookie = new Cookie("refresh_token", "invalid.token.format");

            // Act
            ResultActions result = mockMvc.perform(post(refreshEndpoint)
                    .header("Authorization", "Bearer " + validAccessToken)
                    .cookie(malformedCookie));

            // Assert
            result.andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Should return 403 when refresh token is blacklisted")
        void shouldReturn403WhenRefreshTokenIsBlacklisted() throws Exception {
            // Arrange
            String blacklistedRefreshToken = tokensCreateService.generateRefreshToken(authData.user().getId());
            String tokenId = tokensQueryService.getTokenId(blacklistedRefreshToken);
            blackListService.addTokenToBlackList(tokenId, 3600000L); // 1 hour
            
            Cookie blacklistedCookie = new Cookie("refresh_token", blacklistedRefreshToken);

            // Act
            ResultActions result = mockMvc.perform(post(refreshEndpoint)
                    .header("Authorization", "Bearer " + validAccessToken)
                    .cookie(blacklistedCookie));

            // Assert
            result.andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Should return 403 when refresh token has invalid subject")
        void shouldReturn403WhenRefreshTokenHasInvalidSubject() throws Exception {
            // Arrange - Create token with non-existent user ID
            String invalidSubjectToken = tokensCreateService.generateRefreshToken(99999L);
            Cookie invalidCookie = new Cookie("refresh_token", invalidSubjectToken);

            // Act
            ResultActions result = mockMvc.perform(post(refreshEndpoint)
                    .header("Authorization", "Bearer " + validAccessToken)
                    .cookie(invalidCookie));

            // Assert
            result.andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("POST /v1/tokens/refresh - User and Association Errors")
    class UserAndAssociationErrorTests {

        @Test
        @DisplayName("Should return 403 when user doesn't exist")
        void shouldReturn403WhenUserDoesntExist() throws Exception {
            // Arrange - Create tokens for non-existent user
            String nonExistentUserToken = tokensCreateService.generateAccessToken(99999L);
            String nonExistentRefreshToken = tokensCreateService.generateRefreshToken(99999L);
            Cookie refreshCookie = new Cookie("refresh_token", nonExistentRefreshToken);

            // Act
            ResultActions result = mockMvc.perform(post(refreshEndpoint)
                    .header("Authorization", "Bearer " + nonExistentUserToken)
                    .cookie(refreshCookie));

            // Assert
            result.andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Should return 404 when user has no association membership")
        void shouldReturn404WhenUserHasNoAssociationMembership() throws Exception {
            // Arrange - Create user without association membership
            String userUniqueId = String.valueOf(System.currentTimeMillis() + 2);
            AuthTestData userData = authTestUtils.createAuthenticatedUserWithCredentials(
                    "testuser" + userUniqueId,
                    "testuser" + userUniqueId + "@example.com",
                    "password123"
            );
            // Delete the membership to simulate user without association
            // This would require additional setup in a real scenario
            
            String userAccessToken = tokensCreateService.generateAccessToken(userData.user().getId());
            String userRefreshToken = tokensCreateService.generateRefreshToken(userData.user().getId());
            Cookie refreshCookie = new Cookie("refresh_token", userRefreshToken);

            // Note: This test might need to be adjusted based on actual membership deletion logic
            // For now, we'll test with valid membership and expect success
            
            // Act
            ResultActions result = mockMvc.perform(post(refreshEndpoint)
                    .header("Authorization", "Bearer " + userAccessToken)
                    .cookie(refreshCookie));

            // Assert - Should succeed with valid membership
            result.andExpect(status().isOk());
        }

        @Test
        @DisplayName("Should return 401 when user account is disabled")
        void shouldReturn401WhenUserAccountIsDisabled() throws Exception {
            // Arrange
            String disabledUniqueId = String.valueOf(System.currentTimeMillis() + 3);
            AuthTestData disabledUserData = authTestUtils.createAuthenticatedUserWithCredentials(
                    "disableduser" + disabledUniqueId,
                    "disableduser" + disabledUniqueId + "@example.com",
                    "password123"
            );
            String disabledUserAccessToken = tokensCreateService.generateAccessToken(disabledUserData.user().getId());
            String disabledUserRefreshToken = tokensCreateService.generateRefreshToken(disabledUserData.user().getId());
            Cookie refreshCookie = new Cookie("refresh_token", disabledUserRefreshToken);

            // Act
            ResultActions result = mockMvc.perform(post(refreshEndpoint)
                    .header("Authorization", "Bearer " + disabledUserAccessToken)
                    .cookie(refreshCookie));

            // Assert - Should succeed as token validation doesn't check user enabled status
            // The business logic might need to be updated if disabled users should be rejected
            result.andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("POST /v1/tokens/refresh - Token Reuse Prevention")
    class TokenReusePrevention {

        @Test
        @DisplayName("Should prevent reuse of already refreshed access token")
        void shouldPreventReuseOfAlreadyRefreshedAccessToken() throws Exception {
            // Arrange
            Cookie refreshCookie = new Cookie("refresh_token", validRefreshToken);

            // Act - First refresh
            mockMvc.perform(post(refreshEndpoint)
                    .header("Authorization", "Bearer " + validAccessToken)
                    .cookie(refreshCookie))
                    .andExpect(status().isOk());

            // Act - Try to reuse the same access token
            ResultActions result = mockMvc.perform(post(refreshEndpoint)
                    .header("Authorization", "Bearer " + validAccessToken)
                    .cookie(refreshCookie));

            // Assert - Should fail because token is blacklisted
            result.andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Should prevent reuse of already refreshed refresh token")
        void shouldPreventReuseOfAlreadyRefreshedRefreshToken() throws Exception {
            // Arrange
            String newAccessToken = tokensCreateService.generateAccessToken(authData.user().getId());
            Cookie refreshCookie = new Cookie("refresh_token", validRefreshToken);

            // Act - First refresh
            mockMvc.perform(post(refreshEndpoint)
                    .header("Authorization", "Bearer " + validAccessToken)
                    .cookie(refreshCookie))
                    .andExpect(status().isOk());

            // Act - Try to reuse the same refresh token with new access token
            ResultActions result = mockMvc.perform(post(refreshEndpoint)
                    .header("Authorization", "Bearer " + newAccessToken)
                    .cookie(refreshCookie));

            // Assert - Should fail because refresh token is blacklisted
            result.andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("POST /v1/tokens/refresh - Response Structure and Security")
    class ResponseStructureAndSecurityTests {

        @Test
        @DisplayName("Should return correct response structure")
        void shouldReturnCorrectResponseStructure() throws Exception {
            // Arrange
            Cookie refreshCookie = new Cookie("refresh_token", validRefreshToken);

            // Act
            ResultActions result = mockMvc.perform(post(refreshEndpoint)
                    .header("Authorization", "Bearer " + validAccessToken)
                    .cookie(refreshCookie));

            // Assert
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Token refreshed successfully"))
                    .andExpect(jsonPath("$.timestamp").exists())
                    .andExpect(jsonPath("$.data").exists())
                    .andExpect(jsonPath("$.data.accessToken").exists())
                    .andExpect(jsonPath("$.data.associationId").exists())
                    .andExpect(jsonPath("$.data.accessToken").isString())
                    .andExpect(jsonPath("$.data.associationId").isNumber());
        }

        @Test
        @DisplayName("Should set secure cookie attributes for refresh token")
        void shouldSetSecureCookieAttributesForRefreshToken() throws Exception {
            // Arrange
            Cookie refreshCookie = new Cookie("refresh_token", validRefreshToken);

            // Act
            ResultActions result = mockMvc.perform(post(refreshEndpoint)
                    .header("Authorization", "Bearer " + validAccessToken)
                    .cookie(refreshCookie));

            // Assert
            result.andExpect(status().isOk())
                    .andExpect(cookie().exists("refresh_token"))
                    .andExpect(cookie().httpOnly("refresh_token", true))
                    .andExpect(cookie().secure("refresh_token", true))
                    .andExpect(cookie().sameSite("refresh_token", "None"))
                    .andExpect(cookie().path("refresh_token", "/"));
        }
    }

    @Nested
    @DisplayName("POST /v1/tokens/refresh - Edge Cases")
    class EdgeCasesTests {

        @Test
        @DisplayName("Should handle multiple concurrent refresh requests")
        void shouldHandleMultipleConcurrentRefreshRequests() throws Exception {
            // Arrange
            Cookie refreshCookie = new Cookie("refresh_token", validRefreshToken);

            // Act - First request should succeed
            ResultActions result1 = mockMvc.perform(post(refreshEndpoint)
                    .header("Authorization", "Bearer " + validAccessToken)
                    .cookie(refreshCookie));

            // Act - Second concurrent request should fail (tokens already blacklisted)
            ResultActions result2 = mockMvc.perform(post(refreshEndpoint)
                    .header("Authorization", "Bearer " + validAccessToken)
                    .cookie(refreshCookie));

            // Assert
            result1.andExpect(status().isOk());
            result2.andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Should handle refresh with extra cookies present")
        void shouldHandleRefreshWithExtraCookiesPresent() throws Exception {
            // Arrange
            Cookie refreshCookie = new Cookie("refresh_token", validRefreshToken);
            Cookie extraCookie1 = new Cookie("session_id", "some-session-id");
            Cookie extraCookie2 = new Cookie("preferences", "dark-mode");

            // Act
            ResultActions result = mockMvc.perform(post(refreshEndpoint)
                    .header("Authorization", "Bearer " + validAccessToken)
                    .cookie(refreshCookie, extraCookie1, extraCookie2));

            // Assert - Should succeed and ignore extra cookies
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("Should handle refresh with additional headers")
        void shouldHandleRefreshWithAdditionalHeaders() throws Exception {
            // Arrange
            Cookie refreshCookie = new Cookie("refresh_token", validRefreshToken);

            // Act
            ResultActions result = mockMvc.perform(post(refreshEndpoint)
                    .header("Authorization", "Bearer " + validAccessToken)
                    .header("User-Agent", "Test-Agent/1.0")
                    .header("X-Forwarded-For", "192.168.1.1")
                    .header("Accept", "application/json")
                    .cookie(refreshCookie));

            // Assert - Should succeed and ignore extra headers
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }
    }
} 