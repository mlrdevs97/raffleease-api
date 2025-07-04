package com.raffleease.raffleease.Domains.Raffles.Jobs;

import com.raffleease.raffleease.Base.AbstractIntegrationTest;
import com.raffleease.raffleease.Domains.Raffles.Model.Raffle;
import com.raffleease.raffleease.Domains.Raffles.Repository.RafflesRepository;
import com.raffleease.raffleease.util.AuthTestUtils;
import com.raffleease.raffleease.util.AuthTestUtils.AuthTestData;
import com.raffleease.raffleease.util.TestDataBuilder;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.List;

import static com.raffleease.raffleease.Domains.Raffles.Model.CompletionReason.END_DATE_REACHED;
import static com.raffleease.raffleease.Domains.Raffles.Model.RaffleStatus.ACTIVE;
import static com.raffleease.raffleease.Domains.Raffles.Model.RaffleStatus.COMPLETED;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Raffles Complete Scheduler Integration Tests")
class RafflesCompleteSchedulerIT extends AbstractIntegrationTest {

    @Autowired
    private RafflesCompleteScheduler scheduler;

    @Autowired
    private RafflesRepository rafflesRepository;

    @Autowired
    private AuthTestUtils authTestUtils;

    private AuthTestData authData;
    private List<Raffle> testRaffles;

    @BeforeEach
    void setUp() {
        authData = authTestUtils.createAuthenticatedUser();
        
        // Create test raffles with different end dates
        LocalDateTime now = LocalDateTime.now();
        testRaffles = List.of(
            createTestRaffle("End Date Reached", now.minusMinutes(5), now.minusMinutes(1)),
            createTestRaffle("Current End Date", now.minusSeconds(1), now.minusSeconds(1)),
            createTestRaffle("Future End Date", now.plusHours(1), now.plusHours(2))
        );
    }

    private Raffle createTestRaffle(String title, LocalDateTime startDate, LocalDateTime endDate) {
        Raffle raffle = TestDataBuilder.raffle()
                .association(authData.association())
                .status(ACTIVE)
                .title(title)
                .description("Test raffle for completion")
                .startDate(startDate)
                .endDate(endDate)
                .totalTickets(10L)
                .build();
        
        return rafflesRepository.save(raffle);
    }

    @Nested
    @DisplayName("completeRaffles()")
    class CompleteRafflesTests {

        @Test
        @DisplayName("Should complete raffles with end date reached")
        void shouldCompleteRafflesWithEndDateReached() {
            // Act
            scheduler.completeRaffles();

            // Assert
            List<Raffle> updatedRaffles = rafflesRepository.findAll();
            
            // End date reached raffle should be completed
            Raffle endDateRaffle = findRaffleByTitle(updatedRaffles, "End Date Reached");
            assertThat(endDateRaffle.getStatus()).isEqualTo(COMPLETED);
            assertThat(endDateRaffle.getCompletionReason()).isEqualTo(END_DATE_REACHED);
            assertThat(endDateRaffle.getCompletedAt()).isNotNull();
            
            // Current end date raffle should be completed
            Raffle currentEndDateRaffle = findRaffleByTitle(updatedRaffles, "Current End Date");
            assertThat(currentEndDateRaffle.getStatus()).isEqualTo(COMPLETED);
            assertThat(currentEndDateRaffle.getCompletionReason()).isEqualTo(END_DATE_REACHED);
            assertThat(currentEndDateRaffle.getCompletedAt()).isNotNull();
            
            // Future end date raffle should remain active
            Raffle futureEndDateRaffle = findRaffleByTitle(updatedRaffles, "Future End Date");
            assertThat(futureEndDateRaffle.getStatus()).isEqualTo(ACTIVE);
        }

        @Test
        @DisplayName("Should handle empty list of eligible raffles")
        void shouldHandleEmptyListOfEligibleRaffles() {
            // Arrange - Delete all test raffles
            rafflesRepository.deleteAll();

            // Act
            scheduler.completeRaffles();

            // Assert
            List<Raffle> updatedRaffles = rafflesRepository.findAll();
            assertThat(updatedRaffles).isEmpty();
        }
    }

    private Raffle findRaffleByTitle(List<Raffle> raffles, String title) {
        return raffles.stream()
                .filter(r -> r.getTitle().equals(title))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Raffle with title " + title + " not found"));
    }
} 