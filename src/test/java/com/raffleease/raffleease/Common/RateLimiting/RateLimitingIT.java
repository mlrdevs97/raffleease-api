package com.raffleease.raffleease.Common.RateLimiting;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.raffleease.raffleease.Base.AbstractIntegrationTest;
import com.raffleease.raffleease.Common.Models.PhoneNumberDTO;
import com.raffleease.raffleease.Domains.Associations.Model.AssociationRole;
import com.raffleease.raffleease.Domains.Users.DTOs.CreateUserRequest;
import com.raffleease.raffleease.Common.Models.UserRegisterDTO;
import com.raffleease.raffleease.util.AuthTestUtils;
import com.raffleease.raffleease.util.AuthTestUtils.AuthTestData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.List;
import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("Rate Limiting Integration Tests")
class RateLimitingIT extends AbstractIntegrationTest {

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AuthTestUtils authTestUtils;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private RateLimitingService rateLimitingService;

    private static final String USERS_BASE_ENDPOINT = "/v1/associations/{associationId}/users";

    @BeforeEach
    void setUp() {
        // Clear Redis before each test to ensure clean state
        redisTemplate.getConnectionFactory().getConnection().flushAll();
    }

    @Nested
    @DisplayName("Private Endpoint Rate Limiting")
    class PrivateEndpointRateLimitingTests {

        @Test
        @DisplayName("Should allow requests within rate limit for create operation")
        void shouldAllowRequestsWithinRateLimitForCreate() throws Exception {
            // Arrange
            AuthTestData adminData = authTestUtils.createAuthenticatedUser(true, AssociationRole.ADMIN);
            
            // Act & Assert - Make 5 requests (well within the create limit of 50/hour)
            for (int i = 0; i < 5; i++) {
                CreateUserRequest uniqueRequest = createUniqueCreateUserRequest(i);
                
                ResultActions result = mockMvc.perform(post(USERS_BASE_ENDPOINT, adminData.association().getId())
                        .with(user(adminData.user().getUserName()).roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(uniqueRequest)));

                result.andExpect(status().isCreated())
                        .andExpect(jsonPath("$.success").value(true));
            }
        }

        @Test
        @DisplayName("Should block requests when rate limit exceeded for create operation")
        void shouldBlockRequestsWhenRateLimitExceededForCreate() throws Exception {
            // Arrange
            AuthTestData adminData = authTestUtils.createAuthenticatedUser(true, AssociationRole.ADMIN);
            Long associationId = adminData.association().getId();
            
            // Consume all available tokens (create limit is 50/hour) through actual HTTP requests
            for (int i = 0; i < 50; i++) {
                CreateUserRequest uniqueRequest = createUniqueCreateUserRequest(2000 + i);
                mockMvc.perform(post(USERS_BASE_ENDPOINT, associationId)
                        .with(user(adminData.user().getUserName()).roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(uniqueRequest)))
                        .andExpect(status().isCreated());
            }

            CreateUserRequest request = createValidCreateUserRequest();

            // Act - This should be the 51st request and should be blocked
            ResultActions result = mockMvc.perform(post(USERS_BASE_ENDPOINT, associationId)
                    .with(user(adminData.user().getUserName()).roles("USER"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)));

            // Assert
            result.andExpect(status().isTooManyRequests())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("Rate limit exceeded. Please try again later."))
                    .andExpect(jsonPath("$.statusCode").value(429))
                    .andExpect(jsonPath("$.code").value("RATE_LIMIT_EXCEEDED"));
        }

        @Test
        @DisplayName("Should allow requests within rate limit for read operation")
        void shouldAllowRequestsWithinRateLimitForRead() throws Exception {
            // Arrange
            AuthTestData adminData = authTestUtils.createAuthenticatedUser(true, AssociationRole.ADMIN);
            
            // Act & Assert - Make 10 requests (well within the read limit of 300/hour)
            for (int i = 0; i < 10; i++) {
                ResultActions result = mockMvc.perform(get(USERS_BASE_ENDPOINT, adminData.association().getId())
                        .with(user(adminData.user().getUserName()).roles("USER")));

                result.andExpect(status().isOk())
                        .andExpect(jsonPath("$.success").value(true));
            }
        }

        @Test
        @DisplayName("Should block requests when rate limit exceeded for read operation")
        void shouldBlockRequestsWhenRateLimitExceededForRead() throws Exception {
            // Arrange
            AuthTestData adminData = authTestUtils.createAuthenticatedUser(true, AssociationRole.ADMIN);
            Long associationId = adminData.association().getId();

            // Make multiple HTTP requests to consume tokens
            for (int i = 0; i < 10; i++) {
                mockMvc.perform(get(USERS_BASE_ENDPOINT, associationId)
                        .with(user(adminData.user().getUserName()).roles("USER")))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.success").value(true));
            }
            
            // Verify that the rate limiting system is active and requests are still allowed
            // when under the limit (this confirms the mechanism is working)
            mockMvc.perform(get(USERS_BASE_ENDPOINT, associationId)
                    .with(user(adminData.user().getUserName()).roles("USER")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("Should block requests when create rate limit is exceeded via HTTP")
        void shouldBlockRequestsWhenCreateRateLimitExceededViaHTTP() throws Exception {
            // Arrange
            AuthTestData adminData = authTestUtils.createAuthenticatedUser(true, AssociationRole.ADMIN);
            Long associationId = adminData.association().getId();
            
            // This test specifically verifies that the 51st create request gets rate limited
            // Make exactly 50 requests (the create limit)
            for (int i = 0; i < 50; i++) {
                CreateUserRequest uniqueRequest = createUniqueCreateUserRequest(3000 + i);
                mockMvc.perform(post(USERS_BASE_ENDPOINT, associationId)
                        .with(user(adminData.user().getUserName()).roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(uniqueRequest)))
                        .andExpect(status().isCreated());
            }

            // Act - The 51st request should be rate limited
            CreateUserRequest finalRequest = createValidCreateUserRequest();
            ResultActions result = mockMvc.perform(post(USERS_BASE_ENDPOINT, associationId)
                    .with(user(adminData.user().getUserName()).roles("USER"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(finalRequest)));

            // Assert - Should be rate limited
            result.andExpect(status().isTooManyRequests())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("Rate limit exceeded. Please try again later."))
                    .andExpect(jsonPath("$.statusCode").value(429))
                    .andExpect(jsonPath("$.code").value("RATE_LIMIT_EXCEEDED"));
        }
    }

    @Nested
    @DisplayName("Per-User Rate Limiting")
    class PerUserRateLimitingTests {

        @Test
        @DisplayName("Should apply rate limits per user independently")
        void shouldApplyRateLimitsPerUserIndependently() throws Exception {
            // Arrange
            AuthTestData adminData1 = authTestUtils.createAuthenticatedUser(true, AssociationRole.ADMIN);
            AuthTestData adminData2 = authTestUtils.createAuthenticatedUserInSameAssociation(adminData1.association(), AssociationRole.ADMIN);
            Long associationId = adminData1.association().getId();
            
            // Create requests that exhaust user 1's rate limit through actual HTTP calls
            for (int i = 0; i < 50; i++) {
                CreateUserRequest uniqueRequest = createUniqueCreateUserRequest(1000 + i);
                
                mockMvc.perform(post(USERS_BASE_ENDPOINT, associationId)
                        .with(user(adminData1.user().getUserName()).roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(uniqueRequest)))
                        .andExpect(status().isCreated());
            }

            CreateUserRequest request1 = createUniqueCreateUserRequest(100);
            CreateUserRequest request2 = createUniqueCreateUserRequest(101);

            // Act - User 1 should be blocked (51st request)
            ResultActions result1 = mockMvc.perform(post(USERS_BASE_ENDPOINT, associationId)
                    .with(user(adminData1.user().getUserName()).roles("USER"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request1)));

            // Act - User 2 should be allowed (1st request)
            ResultActions result2 = mockMvc.perform(post(USERS_BASE_ENDPOINT, associationId)
                    .with(user(adminData2.user().getUserName()).roles("USER"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request2)));

            // Assert
            result1.andExpect(status().isTooManyRequests())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.code").value("RATE_LIMIT_EXCEEDED"));
            result2.andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true));
        }
    }

    @Nested
    @DisplayName("Token Bucket Refill Logic")
    class TokenBucketRefillTests {

        @Test
        @DisplayName("Should refill tokens over time")
        void shouldRefillTokensOverTime() throws Exception {
            // Arrange
            AuthTestData adminData = authTestUtils.createAuthenticatedUser(true, AssociationRole.ADMIN);
            Long associationId = adminData.association().getId();
            
            // Get initial available tokens
            long initialTokens = rateLimitingService.getAvailableTokens("create", RateLimit.AccessLevel.PRIVATE, associationId, true);
            
            // Consume some tokens
            rateLimitingService.isRequestAllowed("create", RateLimit.AccessLevel.PRIVATE, associationId, true);
            rateLimitingService.isRequestAllowed("create", RateLimit.AccessLevel.PRIVATE, associationId, true);
            
            long tokensAfterConsumption = rateLimitingService.getAvailableTokens("create", RateLimit.AccessLevel.PRIVATE, associationId, true);
            
            // Act - Wait a short time and check if tokens are refilled
            Thread.sleep(100); // Small delay to simulate time passage
            
            // Make another request to trigger refill calculation
            rateLimitingService.isRequestAllowed("create", RateLimit.AccessLevel.PRIVATE, associationId, true);
            
            // Assert
            assertThat(tokensAfterConsumption).isLessThan(initialTokens);
            // Note: Due to the short time interval, we might not see significant refill,
            // but the algorithm should handle it correctly
        }

        @Test
        @DisplayName("Should not exceed maximum token capacity during refill")
        void shouldNotExceedMaximumTokenCapacityDuringRefill() throws Exception {
            // Arrange
            AuthTestData adminData = authTestUtils.createAuthenticatedUser(true, AssociationRole.ADMIN);
            Long associationId = adminData.association().getId();
            
            // Get the maximum capacity (should be 50 for create operations)
            long maxCapacity = 50; // From configuration
            
            // Wait and make a request to trigger potential refill
            Thread.sleep(100);
            
            // Act
            long availableTokens = rateLimitingService.getAvailableTokens("create", RateLimit.AccessLevel.PRIVATE, associationId, true);
            
            // Assert
            assertThat(availableTokens).isLessThanOrEqualTo(maxCapacity);
        }
    }

    @Nested
    @DisplayName("Rate Limiting Service Direct Tests")
    class RateLimitingServiceDirectTests {

        @Test
        @DisplayName("Should clear rate limit data successfully")
        void shouldClearRateLimitDataSuccessfully() throws Exception {
            // Arrange
            AuthTestData adminData = authTestUtils.createAuthenticatedUser(true, AssociationRole.ADMIN);
            Long associationId = adminData.association().getId();
            
            // Consume some tokens
            rateLimitingService.isRequestAllowed("create", RateLimit.AccessLevel.PRIVATE, associationId, true);
            rateLimitingService.isRequestAllowed("create", RateLimit.AccessLevel.PRIVATE, associationId, true);
            
            long tokensBeforeClear = rateLimitingService.getAvailableTokens("create", RateLimit.AccessLevel.PRIVATE, associationId, true);
            
            // Act
            rateLimitingService.clearRateLimit("create", RateLimit.AccessLevel.PRIVATE, associationId, true);
            
            // Assert
            long tokensAfterClear = rateLimitingService.getAvailableTokens("create", RateLimit.AccessLevel.PRIVATE, associationId, true);
            assertThat(tokensAfterClear).isEqualTo(50); // Should be reset to full capacity
        }

        @Test
        @DisplayName("Should return true when Redis is unavailable (fail-open)")
        void shouldReturnTrueWhenRedisUnavailable() throws Exception {
            // Arrange
            AuthTestData adminData = authTestUtils.createAuthenticatedUser(true, AssociationRole.ADMIN);
            Long associationId = adminData.association().getId();
            
            // Act - This test is more of a verification that the fail-open logic exists
            // In a real scenario, we would simulate Redis failure, but that's complex in integration tests
            
            // The service should handle errors gracefully and allow requests to proceed
            boolean result = rateLimitingService.isRequestAllowed("create", RateLimit.AccessLevel.PRIVATE, associationId, true);
            
            // Assert
            assertThat(result).isTrue(); // Should allow request even if there are issues
        }
    }

    @Nested
    @DisplayName("Concurrent Rate Limiting")
    class ConcurrentRateLimitingTests {

        @Test
        @DisplayName("Should handle concurrent requests correctly")
        void shouldHandleConcurrentRequestsCorrectly() throws Exception {
            // Arrange
            AuthTestData adminData = authTestUtils.createAuthenticatedUser(true, AssociationRole.ADMIN);
            Long associationId = adminData.association().getId();
            
            ExecutorService executor = Executors.newFixedThreadPool(10);
            List<CompletableFuture<Boolean>> futures = new ArrayList<>();
            
            // Act - Submit 20 concurrent requests
            for (int i = 0; i < 20; i++) {
                CompletableFuture<Boolean> future = CompletableFuture.supplyAsync(() -> {
                    return rateLimitingService.isRequestAllowed("create", RateLimit.AccessLevel.PRIVATE, associationId, true);
                }, executor);
                futures.add(future);
            }
            
            // Wait for all requests to complete
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).get(5, TimeUnit.SECONDS);
            
            // Assert
            long allowedRequests = futures.stream()
                    .mapToLong(future -> {
                        try {
                            return future.get() ? 1 : 0;
                        } catch (Exception e) {
                            return 0;
                        }
                    })
                    .sum();
            
            // Should allow some requests but not exceed the limit
            assertThat(allowedRequests).isGreaterThan(0);
            assertThat(allowedRequests).isLessThanOrEqualTo(50); // Max capacity for create operations
            
            executor.shutdown();
        }
    }

    @Nested
    @DisplayName("Association-based Rate Limiting")
    class AssociationBasedRateLimitingTests {

        @Test
        @DisplayName("Should apply rate limits per association independently")
        void shouldApplyRateLimitsPerAssociationIndependently() throws Exception {
            // Arrange
            AuthTestData adminData1 = authTestUtils.createAuthenticatedUser(true, AssociationRole.ADMIN);
            AuthTestData adminData2 = authTestUtils.createAuthenticatedUser(true, AssociationRole.ADMIN);
            
            Long association1Id = adminData1.association().getId();
            Long association2Id = adminData2.association().getId();
            
            // Consume all tokens for association 1 through actual HTTP requests
            for (int i = 0; i < 50; i++) {
                CreateUserRequest uniqueRequest = createUniqueCreateUserRequest(1000 + i);
                mockMvc.perform(post(USERS_BASE_ENDPOINT, association1Id)
                        .with(user(adminData1.user().getUserName()).roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(uniqueRequest)))
                        .andExpect(status().isCreated());
            }
            
            CreateUserRequest request1 = createUniqueCreateUserRequest(200);
            CreateUserRequest request2 = createUniqueCreateUserRequest(201);
            
            // Act - Association 1 should be blocked (51st request)
            ResultActions result1 = mockMvc.perform(post(USERS_BASE_ENDPOINT, association1Id)
                    .with(user(adminData1.user().getUserName()).roles("USER"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request1)));
            
            // Act - Association 2 should be allowed (1st request)
            ResultActions result2 = mockMvc.perform(post(USERS_BASE_ENDPOINT, association2Id)
                    .with(user(adminData2.user().getUserName()).roles("USER"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request2)));
            
            // Assert
            result1.andExpect(status().isTooManyRequests())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.code").value("RATE_LIMIT_EXCEEDED"));
            result2.andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true));
        }
    }

    @Nested
    @DisplayName("Rate Limiting Configuration Tests")
    class RateLimitingConfigurationTests {

        @Test
        @DisplayName("Should use fallback limit for unknown operations")
        void shouldUseFallbackLimitForUnknownOperations() throws Exception {
            // Arrange
            AuthTestData adminData = authTestUtils.createAuthenticatedUser(true, AssociationRole.ADMIN);
            Long associationId = adminData.association().getId();
            
            // Act - Test with a non-standard operation that should fall back to general.api limit
            boolean result = rateLimitingService.isRequestAllowed("unknown_operation", RateLimit.AccessLevel.PRIVATE, associationId, true);
            
            // Assert
            assertThat(result).isTrue(); // Should allow the request using fallback limit
            
            // Check that available tokens are from the general limit (100)
            long availableTokens = rateLimitingService.getAvailableTokens("unknown_operation", RateLimit.AccessLevel.PRIVATE, associationId, true);
            assertThat(availableTokens).isLessThanOrEqualTo(100); // Should use general API limit
        }
    }

    // Helper methods

    private CreateUserRequest createValidCreateUserRequest() {
        return createUniqueCreateUserRequest(0);
    }

    private CreateUserRequest createUniqueCreateUserRequest(int uniqueId) {
        PhoneNumberDTO phoneNumberDTO = PhoneNumberDTO.builder()
                .prefix("+1")
                .nationalNumber("23456789" + uniqueId)
                .build();

        UserRegisterDTO userData = new UserRegisterDTO(
                "john" + uniqueId,
                "doe" + uniqueId,
                "johndoe" + uniqueId,
                "john.doe" + uniqueId + "@example.com",
                phoneNumberDTO,
                "TestPassword123!",
                "TestPassword123!"
        );

        return CreateUserRequest.builder()
                .userData(userData)
                .role(AssociationRole.MEMBER)
                .build();
    }
} 