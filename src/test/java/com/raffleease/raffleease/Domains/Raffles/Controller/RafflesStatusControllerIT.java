package com.raffleease.raffleease.Domains.Raffles.Controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.raffleease.raffleease.Base.AbstractIntegrationTest;
import com.raffleease.raffleease.Domains.Associations.Model.AssociationRole;
import com.raffleease.raffleease.Domains.Raffles.DTOs.StatusUpdate;
import com.raffleease.raffleease.Domains.Raffles.Model.Raffle;
import com.raffleease.raffleease.Domains.Raffles.Model.RaffleStatus;
import com.raffleease.raffleease.Domains.Raffles.Model.CompletionReason;
import com.raffleease.raffleease.Domains.Raffles.Model.RaffleStatistics;
import com.raffleease.raffleease.Domains.Raffles.Repository.RafflesRepository;
import com.raffleease.raffleease.Domains.Tickets.Model.Ticket;
import com.raffleease.raffleease.Domains.Tickets.Model.TicketStatus;
import com.raffleease.raffleease.Domains.Tickets.Repository.TicketsRepository;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static com.raffleease.raffleease.Domains.Raffles.Model.RaffleStatus.ACTIVE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("Raffles Status Controller Integration Tests")
class RafflesStatusControllerIT extends AbstractIntegrationTest {

    @Autowired
    private AuthTestUtils authTestUtils;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RafflesRepository rafflesRepository;

    @Autowired
    private TicketsRepository ticketsRepository;

    private AuthTestData authData;
    private Raffle testRaffle;
    private String statusEndpoint;

    @BeforeEach
    void setUp() {
        authData = authTestUtils.createAuthenticatedUser();
        
        // Create a test raffle with basic setup
        testRaffle = TestDataBuilder.raffle()
                .association(authData.association())
                .status(RaffleStatus.PENDING)
                .title("Test Raffle")
                .description("Test raffle for status updates")
                .endDate(LocalDateTime.now().plusDays(7))
                .build();
        testRaffle = rafflesRepository.save(testRaffle);
        
        statusEndpoint = "/v1/associations/" + authData.association().getId() + "/raffles/" + testRaffle.getId() + "/status";
    }

    @Nested
    @DisplayName("PATCH /v1/associations/{associationId}/raffles/{id}/status")
    class UpdateStatusTests {

        @Nested
        @DisplayName("Status Transition to ACTIVE")
        class ActivateRaffleTests {

            @Test
            @DisplayName("Should successfully activate raffle from PENDING status")
            void shouldActivateRaffleFromPendingStatus() throws Exception {
                // Arrange
                StatusUpdate statusUpdate = new StatusUpdate(ACTIVE);

                // Act
                ResultActions result = mockMvc.perform(patch(statusEndpoint)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(statusUpdate))
                        .with(user(authData.user().getEmail())));

                // Assert
                result.andExpect(status().isOk())
                        .andExpect(content().contentType(APPLICATION_JSON))
                        .andExpect(jsonPath("$.success").value(true))
                        .andExpect(jsonPath("$.message").value("Raffle status updated successfully"))
                        .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                        .andExpect(jsonPath("$.data.startDate").exists());

                // Verify database state
                Raffle updatedRaffle = rafflesRepository.findById(testRaffle.getId()).orElseThrow();
                assertThat(updatedRaffle.getStatus()).isEqualTo(ACTIVE);
                assertThat(updatedRaffle.getStartDate()).isNotNull();
                assertThat(updatedRaffle.getStartDate()).isBeforeOrEqualTo(LocalDateTime.now());
            }

            @Test
            @DisplayName("Should successfully activate raffle from PAUSED status")
            void shouldActivateRaffleFromPausedStatus() throws Exception {
                // Arrange - Set raffle to PAUSED
                testRaffle.setStatus(RaffleStatus.PAUSED);
                testRaffle.setStartDate(LocalDateTime.now().minusHours(2));
                rafflesRepository.save(testRaffle);

                StatusUpdate statusUpdate = new StatusUpdate(ACTIVE);

                // Act
                ResultActions result = mockMvc.perform(patch(statusEndpoint)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(statusUpdate))
                        .with(user(authData.user().getEmail())));

                // Assert
                result.andExpect(status().isOk())
                        .andExpect(jsonPath("$.success").value(true))
                        .andExpect(jsonPath("$.data.status").value("ACTIVE"));

                // Verify database state
                Raffle updatedRaffle = rafflesRepository.findById(testRaffle.getId()).orElseThrow();
                assertThat(updatedRaffle.getStatus()).isEqualTo(ACTIVE);
                // Start date should remain unchanged from when it was first activated
                assertThat(updatedRaffle.getStartDate()).isNotNull();
            }

            @Test
            @DisplayName("Should successfully reactivate raffle from COMPLETED status when conditions are met")
            void shouldReactivateRaffleFromCompletedStatusWhenConditionsMet() throws Exception {
                // Arrange - Setup completed raffle with valid reactivation conditions
                setupRaffleWithTickets(testRaffle, 10L, 1L);
                testRaffle.setStatus(RaffleStatus.COMPLETED);
                testRaffle.setCompletionReason(CompletionReason.MANUALLY_COMPLETED);
                testRaffle.setCompletedAt(LocalDateTime.now().minusHours(1));
                testRaffle.setEndDate(LocalDateTime.now().plusDays(2)); // Valid end date
                rafflesRepository.save(testRaffle);

                StatusUpdate statusUpdate = new StatusUpdate(ACTIVE);

                // Act
                ResultActions result = mockMvc.perform(patch(statusEndpoint)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(statusUpdate))
                        .with(user(authData.user().getEmail())));

                // Assert
                result.andExpect(status().isOk())
                        .andExpect(jsonPath("$.success").value(true))
                        .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                        .andExpect(jsonPath("$.data.completionReason").isEmpty())
                        .andExpect(jsonPath("$.data.completedAt").isEmpty());

                // Verify database state
                Raffle updatedRaffle = rafflesRepository.findById(testRaffle.getId()).orElseThrow();
                assertThat(updatedRaffle.getStatus()).isEqualTo(ACTIVE);
                assertThat(updatedRaffle.getCompletionReason()).isNull();
                assertThat(updatedRaffle.getCompletedAt()).isNull();
            }

            @Test
            @DisplayName("Should return 400 when trying to reactivate raffle that already has a winner")
            void shouldReturn400WhenTryingToReactivateRaffleWithWinner() throws Exception {
                // Arrange - Setup completed raffle with winning ticket
                setupRaffleWithTickets(testRaffle, 5L, 1L);
                Ticket winningTicket = ticketsRepository.findByRaffleAndStatus(testRaffle, TicketStatus.AVAILABLE).get(0);
                testRaffle.setStatus(RaffleStatus.COMPLETED);
                testRaffle.setCompletionReason(CompletionReason.MANUALLY_COMPLETED);
                testRaffle.setCompletedAt(LocalDateTime.now().minusHours(1));
                testRaffle.setWinningTicket(winningTicket);
                testRaffle.setEndDate(LocalDateTime.now().plusDays(2));
                rafflesRepository.save(testRaffle);

                StatusUpdate statusUpdate = new StatusUpdate(ACTIVE);

                // Act
                ResultActions result = mockMvc.perform(patch(statusEndpoint)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(statusUpdate))
                        .with(user(authData.user().getEmail())));

                // Assert
                result.andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.success").value(false))
                        .andExpect(jsonPath("$.message").value("Cannot reactivate a raffle that already has a winner"));

                // Verify raffle remains unchanged
                Raffle unchangedRaffle = rafflesRepository.findById(testRaffle.getId()).orElseThrow();
                assertThat(unchangedRaffle.getStatus()).isEqualTo(RaffleStatus.COMPLETED);
            }

            @Test
            @DisplayName("Should return 400 when trying to reactivate raffle with end date less than 24 hours away")
            void shouldReturn400WhenTryingToReactivateRaffleWithNearEndDate() throws Exception {
                // Arrange - Setup completed raffle with end date too close
                setupRaffleWithTickets(testRaffle, 5L, 1L);
                testRaffle.setStatus(RaffleStatus.COMPLETED);
                testRaffle.setCompletionReason(CompletionReason.END_DATE_REACHED);
                testRaffle.setCompletedAt(LocalDateTime.now().minusHours(1));
                testRaffle.setEndDate(LocalDateTime.now().plusHours(12)); // Less than 24 hours
                rafflesRepository.save(testRaffle);

                StatusUpdate statusUpdate = new StatusUpdate(ACTIVE);

                // Act
                ResultActions result = mockMvc.perform(patch(statusEndpoint)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(statusUpdate))
                        .with(user(authData.user().getEmail())));

                // Assert
                result.andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.success").value(false))
                        .andExpect(jsonPath("$.message").value("The end date of the raffle must be at least one day after the current date to reactivate"));

                // Verify raffle remains unchanged
                Raffle unchangedRaffle = rafflesRepository.findById(testRaffle.getId()).orElseThrow();
                assertThat(unchangedRaffle.getStatus()).isEqualTo(RaffleStatus.COMPLETED);
            }

            @Test
            @DisplayName("Should return 400 when trying to reactivate raffle with no available tickets")
            void shouldReturn400WhenTryingToReactivateRaffleWithNoAvailableTickets() throws Exception {
                // Arrange - Setup completed raffle with all tickets sold
                setupRaffleWithTickets(testRaffle, 5L, 1L);
                sellAllTickets(testRaffle); // Sell all tickets
                testRaffle.setStatus(RaffleStatus.COMPLETED);
                testRaffle.setCompletionReason(CompletionReason.ALL_TICKETS_SOLD);
                testRaffle.setCompletedAt(LocalDateTime.now().minusHours(1));
                testRaffle.setEndDate(LocalDateTime.now().plusDays(2));
                rafflesRepository.save(testRaffle);

                StatusUpdate statusUpdate = new StatusUpdate(ACTIVE);

                // Act
                ResultActions result = mockMvc.perform(patch(statusEndpoint)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(statusUpdate))
                        .with(user(authData.user().getEmail())));

                // Assert
                result.andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.success").value(false))
                        .andExpect(jsonPath("$.message").value("Available tickets for raffle are required to reactivate"));

                // Verify raffle remains unchanged
                Raffle unchangedRaffle = rafflesRepository.findById(testRaffle.getId()).orElseThrow();
                assertThat(unchangedRaffle.getStatus()).isEqualTo(RaffleStatus.COMPLETED);
            }

            @Test
            @DisplayName("Should return 400 when trying to activate from ACTIVE status")
            void shouldReturn400WhenTryingToActivateFromActiveStatus() throws Exception {
                // Arrange - Set raffle to ACTIVE
                testRaffle.setStatus(ACTIVE);
                testRaffle.setStartDate(LocalDateTime.now().minusHours(1));
                rafflesRepository.save(testRaffle);

                StatusUpdate statusUpdate = new StatusUpdate(ACTIVE);

                // Act
                ResultActions result = mockMvc.perform(patch(statusEndpoint)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(statusUpdate))
                        .with(user(authData.user().getEmail())));

                // Assert
                result.andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.success").value(false))
                        .andExpect(jsonPath("$.message").value("Invalid status transition to ACTIVE"));
            }

            @Test
            @DisplayName("Should return 400 when trying to activate PENDING raffle with end date less than 24 hours away")
            void shouldReturn400WhenTryingToActivatePendingRaffleWithNearEndDate() throws Exception {
                // Arrange - Set raffle end date to less than 24 hours from now
                testRaffle.setEndDate(LocalDateTime.now().plusHours(12)); // Only 12 hours away
                rafflesRepository.save(testRaffle);

                StatusUpdate statusUpdate = new StatusUpdate(ACTIVE);

                // Act
                ResultActions result = mockMvc.perform(patch(statusEndpoint)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(statusUpdate))
                        .with(user(authData.user().getEmail())));

                // Assert
                result.andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.success").value(false))
                        .andExpect(jsonPath("$.message").value("The end date of the raffle must be at least one day after the current date to reactivate"));

                // Verify raffle remains in PENDING status
                Raffle unchangedRaffle = rafflesRepository.findById(testRaffle.getId()).orElseThrow();
                assertThat(unchangedRaffle.getStatus()).isEqualTo(RaffleStatus.PENDING);
                assertThat(unchangedRaffle.getStartDate()).isNull(); // Should not have been set
            }

            @Test
            @DisplayName("Should return 400 when trying to activate PENDING raffle with end date exactly 24 hours away")
            void shouldReturn400WhenTryingToActivatePendingRaffleWithEndDateExactly24HoursAway() throws Exception {
                // Arrange - Set raffle end date to exactly 24 hours from now (should still fail due to "isBefore" check)
                testRaffle.setEndDate(LocalDateTime.now().plusHours(24));
                rafflesRepository.save(testRaffle);

                StatusUpdate statusUpdate = new StatusUpdate(ACTIVE);

                // Act
                ResultActions result = mockMvc.perform(patch(statusEndpoint)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(statusUpdate))
                        .with(user(authData.user().getEmail())));

                // Assert
                result.andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.success").value(false))
                        .andExpect(jsonPath("$.message").value("The end date of the raffle must be at least one day after the current date to reactivate"));

                // Verify raffle remains in PENDING status
                Raffle unchangedRaffle = rafflesRepository.findById(testRaffle.getId()).orElseThrow();
                assertThat(unchangedRaffle.getStatus()).isEqualTo(RaffleStatus.PENDING);
                assertThat(unchangedRaffle.getStartDate()).isNull();
            }

            @Test
            @DisplayName("Should successfully activate PENDING raffle with end date more than 24 hours away")
            void shouldSuccessfullyActivatePendingRaffleWithValidEndDate() throws Exception {
                // Arrange - Set raffle end date to more than 24 hours from now
                testRaffle.setEndDate(LocalDateTime.now().plusHours(25)); // 25 hours away
                rafflesRepository.save(testRaffle);

                StatusUpdate statusUpdate = new StatusUpdate(ACTIVE);

                // Act
                ResultActions result = mockMvc.perform(patch(statusEndpoint)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(statusUpdate))
                        .with(user(authData.user().getEmail())));

                // Assert
                result.andExpect(status().isOk())
                        .andExpect(jsonPath("$.success").value(true))
                        .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                        .andExpect(jsonPath("$.data.startDate").exists());

                // Verify database state
                Raffle updatedRaffle = rafflesRepository.findById(testRaffle.getId()).orElseThrow();
                assertThat(updatedRaffle.getStatus()).isEqualTo(ACTIVE);
                assertThat(updatedRaffle.getStartDate()).isNotNull();
                assertThat(updatedRaffle.getStartDate()).isBeforeOrEqualTo(LocalDateTime.now());
            }

            @Test
            @DisplayName("Should return 400 when trying to activate PAUSED raffle with end date less than 24 hours away")
            void shouldReturn400WhenTryingToActivatePausedRaffleWithNearEndDate() throws Exception {
                // Arrange - Set raffle to PAUSED with end date less than 24 hours away
                testRaffle.setStatus(RaffleStatus.PAUSED);
                testRaffle.setStartDate(LocalDateTime.now().minusHours(2)); // Previously activated
                testRaffle.setEndDate(LocalDateTime.now().plusHours(18)); // Only 18 hours away
                rafflesRepository.save(testRaffle);

                StatusUpdate statusUpdate = new StatusUpdate(ACTIVE);

                // Act
                ResultActions result = mockMvc.perform(patch(statusEndpoint)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(statusUpdate))
                        .with(user(authData.user().getEmail())));

                // Assert
                result.andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.success").value(false))
                        .andExpect(jsonPath("$.message").value("The end date of the raffle must be at least one day after the current date to reactivate"));

                // Verify raffle remains in PAUSED status
                Raffle unchangedRaffle = rafflesRepository.findById(testRaffle.getId()).orElseThrow();
                assertThat(unchangedRaffle.getStatus()).isEqualTo(RaffleStatus.PAUSED);
                assertThat(unchangedRaffle.getStartDate()).isNotNull(); // Should preserve original start date
            }

            @Test
            @DisplayName("Should successfully activate PAUSED raffle with end date more than 24 hours away")
            void shouldSuccessfullyActivatePausedRaffleWithValidEndDate() throws Exception {
                // Arrange - Set raffle to PAUSED with valid end date
                testRaffle.setStatus(RaffleStatus.PAUSED);
                testRaffle.setStartDate(LocalDateTime.now().minusHours(2)); // Previously activated
                testRaffle.setEndDate(LocalDateTime.now().plusHours(30)); // 30 hours away
                rafflesRepository.save(testRaffle);

                StatusUpdate statusUpdate = new StatusUpdate(ACTIVE);

                // Act
                ResultActions result = mockMvc.perform(patch(statusEndpoint)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(statusUpdate))
                        .with(user(authData.user().getEmail())));

                // Assert
                result.andExpect(status().isOk())
                        .andExpect(jsonPath("$.success").value(true))
                        .andExpect(jsonPath("$.data.status").value("ACTIVE"));

                // Verify database state
                Raffle updatedRaffle = rafflesRepository.findById(testRaffle.getId()).orElseThrow();
                assertThat(updatedRaffle.getStatus()).isEqualTo(ACTIVE);
                assertThat(updatedRaffle.getStartDate()).isNotNull(); // Should preserve original start date
            }
        }

        @Nested
        @DisplayName("Status Transition to PAUSED")
        class PauseRaffleTests {

            @Test
            @DisplayName("Should successfully pause raffle from ACTIVE status")
            void shouldPauseRaffleFromActiveStatus() throws Exception {
                // Arrange - Set raffle to ACTIVE
                testRaffle.setStatus(ACTIVE);
                testRaffle.setStartDate(LocalDateTime.now().minusHours(1));
                rafflesRepository.save(testRaffle);

                StatusUpdate statusUpdate = new StatusUpdate(RaffleStatus.PAUSED);

                // Act
                ResultActions result = mockMvc.perform(patch(statusEndpoint)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(statusUpdate))
                        .with(user(authData.user().getEmail())));

                // Assert
                result.andExpect(status().isOk())
                        .andExpect(jsonPath("$.success").value(true))
                        .andExpect(jsonPath("$.data.status").value("PAUSED"));

                // Verify database state
                Raffle updatedRaffle = rafflesRepository.findById(testRaffle.getId()).orElseThrow();
                assertThat(updatedRaffle.getStatus()).isEqualTo(RaffleStatus.PAUSED);
                assertThat(updatedRaffle.getStartDate()).isNotNull(); // Should preserve start date
            }

            @Test
            @DisplayName("Should return 400 when trying to pause raffle from PENDING status")
            void shouldReturn400WhenTryingToPauseFromPendingStatus() throws Exception {
                // Arrange - Raffle is already PENDING
                StatusUpdate statusUpdate = new StatusUpdate(RaffleStatus.PAUSED);

                // Act
                ResultActions result = mockMvc.perform(patch(statusEndpoint)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(statusUpdate))
                        .with(user(authData.user().getEmail())));

                // Assert
                result.andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.success").value(false))
                        .andExpect(jsonPath("$.message").value("Only raffles in 'ACTIVE' state can be paused."));

                // Verify raffle remains unchanged
                Raffle unchangedRaffle = rafflesRepository.findById(testRaffle.getId()).orElseThrow();
                assertThat(unchangedRaffle.getStatus()).isEqualTo(RaffleStatus.PENDING);
            }

            @Test
            @DisplayName("Should return 400 when trying to pause raffle from COMPLETED status")
            void shouldReturn400WhenTryingToPauseFromCompletedStatus() throws Exception {
                // Arrange - Set raffle to COMPLETED
                testRaffle.setStatus(RaffleStatus.COMPLETED);
                testRaffle.setCompletionReason(CompletionReason.MANUALLY_COMPLETED);
                testRaffle.setCompletedAt(LocalDateTime.now().minusHours(1));
                rafflesRepository.save(testRaffle);

                StatusUpdate statusUpdate = new StatusUpdate(RaffleStatus.PAUSED);

                // Act
                ResultActions result = mockMvc.perform(patch(statusEndpoint)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(statusUpdate))
                        .with(user(authData.user().getEmail())));

                // Assert
                result.andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.success").value(false))
                        .andExpect(jsonPath("$.message").value("Only raffles in 'ACTIVE' state can be paused."));

                // Verify raffle remains unchanged
                Raffle unchangedRaffle = rafflesRepository.findById(testRaffle.getId()).orElseThrow();
                assertThat(unchangedRaffle.getStatus()).isEqualTo(RaffleStatus.COMPLETED);
            }

            @Test
            @DisplayName("Should return 400 when trying to pause already paused raffle")
            void shouldReturn400WhenTryingToPauseAlreadyPausedRaffle() throws Exception {
                // Arrange - Set raffle to PAUSED
                testRaffle.setStatus(RaffleStatus.PAUSED);
                rafflesRepository.save(testRaffle);

                StatusUpdate statusUpdate = new StatusUpdate(RaffleStatus.PAUSED);

                // Act
                ResultActions result = mockMvc.perform(patch(statusEndpoint)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(statusUpdate))
                        .with(user(authData.user().getEmail())));

                // Assert
                result.andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.success").value(false))
                        .andExpect(jsonPath("$.message").value("Only raffles in 'ACTIVE' state can be paused."));
            }
        }

        @Nested
        @DisplayName("Status Transition to COMPLETED")
        class CompleteRaffleTests {

            @Test
            @DisplayName("Should successfully complete raffle from ACTIVE status")
            void shouldCompleteRaffleFromActiveStatus() throws Exception {
                // Arrange - Set raffle to ACTIVE
                testRaffle.setStatus(ACTIVE);
                testRaffle.setStartDate(LocalDateTime.now().minusHours(2));
                rafflesRepository.save(testRaffle);

                StatusUpdate statusUpdate = new StatusUpdate(RaffleStatus.COMPLETED);

                // Act
                ResultActions result = mockMvc.perform(patch(statusEndpoint)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(statusUpdate))
                        .with(user(authData.user().getEmail())));

                // Assert
                result.andExpect(status().isOk())
                        .andExpect(jsonPath("$.success").value(true))
                        .andExpect(jsonPath("$.data.status").value("COMPLETED"))
                        .andExpect(jsonPath("$.data.completionReason").value("MANUALLY_COMPLETED"))
                        .andExpect(jsonPath("$.data.completedAt").exists());

                // Verify database state
                Raffle updatedRaffle = rafflesRepository.findById(testRaffle.getId()).orElseThrow();
                assertThat(updatedRaffle.getStatus()).isEqualTo(RaffleStatus.COMPLETED);
                assertThat(updatedRaffle.getCompletionReason()).isEqualTo(CompletionReason.MANUALLY_COMPLETED);
                assertThat(updatedRaffle.getCompletedAt()).isNotNull();
                assertThat(updatedRaffle.getCompletedAt()).isBeforeOrEqualTo(LocalDateTime.now());
            }

            @Test
            @DisplayName("Should successfully complete raffle from PAUSED status")
            void shouldCompleteRaffleFromPausedStatus() throws Exception {
                // Arrange - Set raffle to PAUSED
                testRaffle.setStatus(RaffleStatus.PAUSED);
                testRaffle.setStartDate(LocalDateTime.now().minusHours(2));
                rafflesRepository.save(testRaffle);

                StatusUpdate statusUpdate = new StatusUpdate(RaffleStatus.COMPLETED);

                // Act
                ResultActions result = mockMvc.perform(patch(statusEndpoint)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(statusUpdate))
                        .with(user(authData.user().getEmail())));

                // Assert
                result.andExpect(status().isOk())
                        .andExpect(jsonPath("$.success").value(true))
                        .andExpect(jsonPath("$.data.status").value("COMPLETED"))
                        .andExpect(jsonPath("$.data.completionReason").value("MANUALLY_COMPLETED"));

                // Verify database state
                Raffle updatedRaffle = rafflesRepository.findById(testRaffle.getId()).orElseThrow();
                assertThat(updatedRaffle.getStatus()).isEqualTo(RaffleStatus.COMPLETED);
                assertThat(updatedRaffle.getCompletionReason()).isEqualTo(CompletionReason.MANUALLY_COMPLETED);
                assertThat(updatedRaffle.getCompletedAt()).isNotNull();
            }

            @Test
            @DisplayName("Should return 400 when trying to complete raffle from PENDING status")
            void shouldReturn400WhenTryingToCompleteFromPendingStatus() throws Exception {
                // Arrange - Raffle is already PENDING
                StatusUpdate statusUpdate = new StatusUpdate(RaffleStatus.COMPLETED);

                // Act
                ResultActions result = mockMvc.perform(patch(statusEndpoint)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(statusUpdate))
                        .with(user(authData.user().getEmail())));

                // Assert
                result.andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.success").value(false))
                        .andExpect(jsonPath("$.message").value("Cannot complete raffle unless it is active or paused"));

                // Verify raffle remains unchanged
                Raffle unchangedRaffle = rafflesRepository.findById(testRaffle.getId()).orElseThrow();
                assertThat(unchangedRaffle.getStatus()).isEqualTo(RaffleStatus.PENDING);
            }

            @Test
            @DisplayName("Should return 400 when trying to complete already completed raffle")
            void shouldReturn400WhenTryingToCompleteAlreadyCompletedRaffle() throws Exception {
                // Arrange - Set raffle to COMPLETED
                testRaffle.setStatus(RaffleStatus.COMPLETED);
                testRaffle.setCompletionReason(CompletionReason.END_DATE_REACHED);
                testRaffle.setCompletedAt(LocalDateTime.now().minusHours(1));
                rafflesRepository.save(testRaffle);

                StatusUpdate statusUpdate = new StatusUpdate(RaffleStatus.COMPLETED);

                // Act
                ResultActions result = mockMvc.perform(patch(statusEndpoint)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(statusUpdate))
                        .with(user(authData.user().getEmail())));

                // Assert
                result.andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.success").value(false))
                        .andExpect(jsonPath("$.message").value("Cannot complete raffle unless it is active or paused"));
            }
        }

        @Nested
        @DisplayName("Invalid Status Transitions")
        class InvalidStatusTransitionTests {

            @Test
            @DisplayName("Should return 400 when trying to revert to PENDING status")
            void shouldReturn400WhenTryingToRevertToPendingStatus() throws Exception {
                // Arrange - Set raffle to ACTIVE first
                testRaffle.setStatus(ACTIVE);
                testRaffle.setStartDate(LocalDateTime.now().minusHours(1));
                rafflesRepository.save(testRaffle);

                StatusUpdate statusUpdate = new StatusUpdate(RaffleStatus.PENDING);

                // Act
                ResultActions result = mockMvc.perform(patch(statusEndpoint)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(statusUpdate))
                        .with(user(authData.user().getEmail())));

                // Assert
                result.andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.success").value(false))
                        .andExpect(jsonPath("$.message").value("Cannot revert to 'PENDING' state."));

                // Verify raffle remains unchanged
                Raffle unchangedRaffle = rafflesRepository.findById(testRaffle.getId()).orElseThrow();
                assertThat(unchangedRaffle.getStatus()).isEqualTo(ACTIVE);
            }

            @Test
            @DisplayName("Should return 400 when status is null")
            void shouldReturn400WhenStatusIsNull() throws Exception {
                // Arrange
                String invalidStatusJson = """
                    {
                        "status": null
                    }
                    """;

                // Act
                ResultActions result = mockMvc.perform(patch(statusEndpoint)
                        .contentType(APPLICATION_JSON)
                        .content(invalidStatusJson)
                        .with(user(authData.user().getEmail())));

                // Assert
                result.andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.success").value(false));
            }

            @Test
            @DisplayName("Should return 400 when status is missing")
            void shouldReturn400WhenStatusIsMissing() throws Exception {
                // Arrange
                String emptyJson = "{}";

                // Act
                ResultActions result = mockMvc.perform(patch(statusEndpoint)
                        .contentType(APPLICATION_JSON)
                        .content(emptyJson)
                        .with(user(authData.user().getEmail())));

                // Assert
                result.andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.success").value(false));
            }
        }

        @Nested
        @DisplayName("Authorization and Error Handling")
        class AuthorizationAndErrorHandlingTests {

            @Test
            @DisplayName("Should return 401 when user is not authenticated")
            void shouldReturn401WhenNotAuthenticated() throws Exception {
                // Arrange
                StatusUpdate statusUpdate = new StatusUpdate(ACTIVE);

                // Act
                ResultActions result = mockMvc.perform(patch(statusEndpoint)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(statusUpdate)));

                // Assert
                result.andExpect(status().isUnauthorized());
            }

            @Test
            @DisplayName("Should return 403 when user doesn't belong to association")
            void shouldReturn403WhenUserDoesntBelongToAssociation() throws Exception {
                // Arrange
                AuthTestData otherUserData = authTestUtils.createAuthenticatedUserWithCredentials(
                        "otheruser", "other@example.com", "password123");
                
                StatusUpdate statusUpdate = new StatusUpdate(ACTIVE);

                // Act
                ResultActions result = mockMvc.perform(patch(statusEndpoint)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(statusUpdate))
                        .with(user(otherUserData.user().getEmail())));

                // Assert
                result.andExpect(status().isForbidden());
            }

            @Test
            @DisplayName("Should return 403 when COLLABORATOR tries to update raffle status")
            void shouldReturn403WhenCollaboratorTriesToUpdateRaffleStatus() throws Exception {
                // Arrange
                AuthTestData collaboratorData = authTestUtils.createAuthenticatedUserInSameAssociation(
                        authData.association(), AssociationRole.COLLABORATOR);
                
                StatusUpdate statusUpdate = new StatusUpdate(ACTIVE);

                // Act
                ResultActions result = mockMvc.perform(patch(statusEndpoint)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(statusUpdate))
                        .with(user(collaboratorData.user().getEmail())));

                // Assert
                result.andExpect(status().isForbidden())
                        .andExpect(content().contentType(APPLICATION_JSON))
                        .andExpect(jsonPath("$.success").value(false))
                        .andExpect(jsonPath("$.message").value("Only administrators and members can update raffle status"));

                // Verify raffle status remains unchanged
                Raffle unchangedRaffle = rafflesRepository.findById(testRaffle.getId()).orElseThrow();
                assertThat(unchangedRaffle.getStatus()).isEqualTo(RaffleStatus.PENDING);
            }

            @Test
            @DisplayName("Should successfully update raffle status for ADMIN role")
            void shouldSuccessfullyUpdateRaffleStatusForAdmin() throws Exception {
                // Arrange
                AuthTestData adminData = authTestUtils.createAuthenticatedUserInSameAssociation(
                        authData.association(), AssociationRole.ADMIN);
                
                StatusUpdate statusUpdate = new StatusUpdate(ACTIVE);

                // Act
                ResultActions result = mockMvc.perform(patch(statusEndpoint)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(statusUpdate))
                        .with(user(adminData.user().getEmail())));

                // Assert
                result.andExpect(status().isOk())
                        .andExpect(content().contentType(APPLICATION_JSON))
                        .andExpect(jsonPath("$.success").value(true))
                        .andExpect(jsonPath("$.message").value("Raffle status updated successfully"))
                        .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                        .andExpect(jsonPath("$.data.startDate").exists());

                // Verify database state
                Raffle updatedRaffle = rafflesRepository.findById(testRaffle.getId()).orElseThrow();
                assertThat(updatedRaffle.getStatus()).isEqualTo(ACTIVE);
                assertThat(updatedRaffle.getStartDate()).isNotNull();
            }

            @Test
            @DisplayName("Should successfully update raffle status for MEMBER role")
            void shouldSuccessfullyUpdateRaffleStatusForMember() throws Exception {
                // Arrange
                AuthTestData memberData = authTestUtils.createAuthenticatedUserInSameAssociation(
                        authData.association(), AssociationRole.MEMBER);
                
                StatusUpdate statusUpdate = new StatusUpdate(ACTIVE);

                // Act
                ResultActions result = mockMvc.perform(patch(statusEndpoint)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(statusUpdate))
                        .with(user(memberData.user().getEmail())));

                // Assert
                result.andExpect(status().isOk())
                        .andExpect(content().contentType(APPLICATION_JSON))
                        .andExpect(jsonPath("$.success").value(true))
                        .andExpect(jsonPath("$.message").value("Raffle status updated successfully"))
                        .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                        .andExpect(jsonPath("$.data.startDate").exists());

                // Verify database state
                Raffle updatedRaffle = rafflesRepository.findById(testRaffle.getId()).orElseThrow();
                assertThat(updatedRaffle.getStatus()).isEqualTo(ACTIVE);
                assertThat(updatedRaffle.getStartDate()).isNotNull();
            }

            @Test
            @DisplayName("Should return 404 when raffle doesn't exist")
            void shouldReturn404WhenRaffleDoesntExist() throws Exception {
                // Arrange
                String nonExistentRaffleEndpoint = "/v1/associations/" + authData.association().getId() + "/raffles/99999/status";
                StatusUpdate statusUpdate = new StatusUpdate(ACTIVE);

                // Act
                ResultActions result = mockMvc.perform(patch(nonExistentRaffleEndpoint)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(statusUpdate))
                        .with(user(authData.user().getEmail())));

                // Assert
                result.andExpect(status().isNotFound());
            }

            @Test
            @DisplayName("Should return 404 when association doesn't exist")
            void shouldReturn404WhenAssociationDoesntExist() throws Exception {
                // Arrange
                String nonExistentAssociationEndpoint = "/v1/associations/99999/raffles/" + testRaffle.getId() + "/status";
                StatusUpdate statusUpdate = new StatusUpdate(ACTIVE);

                // Act
                ResultActions result = mockMvc.perform(patch(nonExistentAssociationEndpoint)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(statusUpdate))
                        .with(user(authData.user().getEmail())));

                // Assert
                result.andExpect(status().isForbidden()); // Association validation should trigger first
            }
        }
    }

    // Helper Methods

    private void setupRaffleWithTickets(Raffle raffle, Long totalTickets, Long firstTicketNumber) {
        // Set raffle properties
        raffle.setTotalTickets(totalTickets);
        raffle.setFirstTicketNumber(firstTicketNumber);
        
        // Initialize statistics if not present
        if (raffle.getStatistics() == null) {
            raffle.setStatistics(RaffleStatistics.builder()
                    .availableTickets(totalTickets)
                    .soldTickets(0L)
                    .revenue(BigDecimal.ZERO)
                    .totalOrders(0L)
                    .participants(0L)
                    .build());
        } else {
            raffle.getStatistics().setAvailableTickets(totalTickets);
            raffle.getStatistics().setSoldTickets(0L);
        }
        
        rafflesRepository.save(raffle);
        
        // Create tickets
        for (long i = 0; i < totalTickets; i++) {
            Ticket ticket = Ticket.builder()
                    .raffle(raffle)
                    .ticketNumber(String.valueOf(firstTicketNumber + i))
                    .status(TicketStatus.AVAILABLE)
                    .build();
            ticketsRepository.save(ticket);
        }
    }

    private void sellAllTickets(Raffle raffle) {
        List<Ticket> availableTickets = ticketsRepository.findByRaffleAndStatus(raffle, TicketStatus.AVAILABLE);
        
        for (Ticket ticket : availableTickets) {
            ticket.setStatus(TicketStatus.SOLD);
            ticketsRepository.save(ticket);
        }
        
        // Update statistics
        raffle.getStatistics().setSoldTickets((long) availableTickets.size());
        raffle.getStatistics().setAvailableTickets(0L);
        rafflesRepository.save(raffle);
    }
} 