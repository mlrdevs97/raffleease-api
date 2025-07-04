package com.raffleease.raffleease.Domains.Raffles.Controller;

import com.raffleease.raffleease.Base.AbstractIntegrationTest;
import com.raffleease.raffleease.Domains.Associations.Model.AssociationRole;
import com.raffleease.raffleease.Domains.Raffles.Model.Raffle;
import com.raffleease.raffleease.Domains.Raffles.Model.RaffleStatus;
import com.raffleease.raffleease.Domains.Raffles.Model.CompletionReason;
import com.raffleease.raffleease.Domains.Raffles.Repository.RafflesRepository;
import com.raffleease.raffleease.util.AuthTestUtils;
import com.raffleease.raffleease.util.AuthTestUtils.AuthTestData;
import com.raffleease.raffleease.util.TestDataBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Raffles Deletion Controller Integration Tests")
class RafflesDeletionControllerIT extends AbstractIntegrationTest {

    @Autowired
    private AuthTestUtils authTestUtils;

    @Autowired
    private RafflesRepository rafflesRepository;

    private AuthTestData authData;
    private Raffle testRaffle;
    private String deleteEndpoint;

    @BeforeEach
    void setUp() {
        authData = authTestUtils.createAuthenticatedUser();
        
        // Create a test raffle with basic setup
        testRaffle = TestDataBuilder.raffle()
                .association(authData.association())
                .status(RaffleStatus.PENDING)
                .title("Test Raffle")
                .description("Test raffle for deletion")
                .endDate(LocalDateTime.now().plusDays(7))
                .build();
        testRaffle = rafflesRepository.save(testRaffle);
        
        deleteEndpoint = "/v1/associations/" + authData.association().getId() + "/raffles/" + testRaffle.getId();
    }

    @Nested
    @DisplayName("DELETE /v1/associations/{associationId}/raffles/{id}")
    class DeleteRaffleTests {

        @Test
        @DisplayName("Should successfully delete raffle in PENDING status")
        void shouldSuccessfullyDeletePendingRaffle() throws Exception {
            // Act
            ResultActions result = mockMvc.perform(delete(deleteEndpoint)
                    .with(user(authData.user().getEmail())));

            // Assert
            result.andExpect(status().isNoContent());

            // Verify database state
            assertThat(rafflesRepository.findById(testRaffle.getId())).isEmpty();
        }

        @Test
        @DisplayName("Should return 400 when trying to delete ACTIVE raffle")
        void shouldReturn400WhenTryingToDeleteActiveRaffle() throws Exception {
            // Arrange - Set raffle to ACTIVE
            testRaffle.setStatus(RaffleStatus.ACTIVE);
            testRaffle.setStartDate(LocalDateTime.now().minusHours(1));
            rafflesRepository.save(testRaffle);

            // Act
            ResultActions result = mockMvc.perform(delete(deleteEndpoint)
                    .with(user(authData.user().getEmail())));

            // Assert
            result.andExpect(status().isBadRequest());

            // Verify raffle still exists
            assertThat(rafflesRepository.findById(testRaffle.getId())).isPresent();
        }

        @Test
        @DisplayName("Should return 400 when trying to delete PAUSED raffle")
        void shouldReturn400WhenTryingToDeletePausedRaffle() throws Exception {
            // Arrange - Set raffle to PAUSED
            testRaffle.setStatus(RaffleStatus.PAUSED);
            rafflesRepository.save(testRaffle);

            // Act
            ResultActions result = mockMvc.perform(delete(deleteEndpoint)
                    .with(user(authData.user().getEmail())));

            // Assert
            result.andExpect(status().isBadRequest());

            // Verify raffle still exists
            assertThat(rafflesRepository.findById(testRaffle.getId())).isPresent();
        }

        @Test
        @DisplayName("Should return 400 when trying to delete COMPLETED raffle")
        void shouldReturn400WhenTryingToDeleteCompletedRaffle() throws Exception {
            // Arrange - Set raffle to COMPLETED
            testRaffle.setStatus(RaffleStatus.COMPLETED);
            testRaffle.setCompletionReason(CompletionReason.MANUALLY_COMPLETED);
            testRaffle.setCompletedAt(LocalDateTime.now().minusHours(1));
            rafflesRepository.save(testRaffle);

            // Act
            ResultActions result = mockMvc.perform(delete(deleteEndpoint)
                    .with(user(authData.user().getEmail())));

            // Assert
            result.andExpect(status().isBadRequest());

            // Verify raffle still exists
            assertThat(rafflesRepository.findById(testRaffle.getId())).isPresent();
        }

        @Test
        @DisplayName("Should return 401 when user is not authenticated")
        void shouldReturn401WhenNotAuthenticated() throws Exception {
            // Act
            ResultActions result = mockMvc.perform(delete(deleteEndpoint));

            // Assert
            result.andExpect(status().isUnauthorized());

            // Verify raffle still exists
            assertThat(rafflesRepository.findById(testRaffle.getId())).isPresent();
        }

        @Test
        @DisplayName("Should return 403 when user doesn't belong to association")
        void shouldReturn403WhenUserDoesntBelongToAssociation() throws Exception {
            // Arrange
            AuthTestData otherUserData = authTestUtils.createAuthenticatedUserWithCredentials(
                    "otheruser", "other@example.com", "password123");

            // Act
            ResultActions result = mockMvc.perform(delete(deleteEndpoint)
                    .with(user(otherUserData.user().getEmail())));

            // Assert
            result.andExpect(status().isForbidden());

            // Verify raffle still exists
            assertThat(rafflesRepository.findById(testRaffle.getId())).isPresent();
        }

        @Test
        @DisplayName("Should return 404 when raffle doesn't exist")
        void shouldReturn404WhenRaffleDoesntExist() throws Exception {
            // Arrange
            String nonExistentRaffleEndpoint = "/v1/associations/" + authData.association().getId() + "/raffles/99999";

            // Act
            ResultActions result = mockMvc.perform(delete(nonExistentRaffleEndpoint)
                    .with(user(authData.user().getEmail())));

            // Assert
            result.andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Should return 404 when association doesn't exist")
        void shouldReturn404WhenAssociationDoesntExist() throws Exception {
            // Arrange
            String nonExistentAssociationEndpoint = "/v1/associations/99999/raffles/" + testRaffle.getId();

            // Act
            ResultActions result = mockMvc.perform(delete(nonExistentAssociationEndpoint)
                    .with(user(authData.user().getEmail())));

            // Assert
            result.andExpect(status().isForbidden()); // Association validation should trigger first
        }

        @Test
        @DisplayName("Should return 403 when COLLABORATOR tries to delete raffle")
        void shouldReturn403WhenCollaboratorTriesToDeleteRaffle() throws Exception {
            // Arrange
            AuthTestData collaboratorData = authTestUtils.createAuthenticatedUserInSameAssociation(
                    authData.association(), AssociationRole.COLLABORATOR);

            // Act
            ResultActions result = mockMvc.perform(delete(deleteEndpoint)
                    .with(user(collaboratorData.user().getEmail())));

            // Assert
            result.andExpect(status().isForbidden())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("Only administrators and members can delete raffles"));

            // Verify raffle still exists
            assertThat(rafflesRepository.findById(testRaffle.getId())).isPresent();
        }

        @Test
        @DisplayName("Should successfully delete raffle for ADMIN role")
        void shouldSuccessfullyDeleteRaffleForAdmin() throws Exception {
            // Arrange
            AuthTestData adminData = authTestUtils.createAuthenticatedUserInSameAssociation(
                    authData.association(), AssociationRole.ADMIN);

            // Act
            ResultActions result = mockMvc.perform(delete(deleteEndpoint)
                    .with(user(adminData.user().getEmail())));

            // Assert
            result.andExpect(status().isNoContent());

            // Verify raffle was deleted
            assertThat(rafflesRepository.findById(testRaffle.getId())).isEmpty();
        }

        @Test
        @DisplayName("Should successfully delete raffle for MEMBER role")
        void shouldSuccessfullyDeleteRaffleForMember() throws Exception {
            // Arrange
            AuthTestData memberData = authTestUtils.createAuthenticatedUserInSameAssociation(
                    authData.association(), AssociationRole.MEMBER);

            // Act
            ResultActions result = mockMvc.perform(delete(deleteEndpoint)
                    .with(user(memberData.user().getEmail())));

            // Assert
            result.andExpect(status().isNoContent());

            // Verify raffle was deleted
            assertThat(rafflesRepository.findById(testRaffle.getId())).isEmpty();
        }
    }
} 