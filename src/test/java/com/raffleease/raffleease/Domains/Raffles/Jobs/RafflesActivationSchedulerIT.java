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
import org.springframework.test.context.TestPropertySource;

import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.List;

import static com.raffleease.raffleease.Domains.Raffles.Model.RaffleStatus.ACTIVE;
import static com.raffleease.raffleease.Domains.Raffles.Model.RaffleStatus.PENDING;
import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@DisplayName("Raffles Activation Scheduler Integration Tests")
class RafflesActivationSchedulerIT extends AbstractIntegrationTest {

    @Autowired
    private RafflesActivationScheduler scheduler;

    @Autowired
    private RafflesRepository rafflesRepository;

    @Autowired
    private AuthTestUtils authTestUtils;

    private AuthTestData authData;
    private List<Raffle> testRaffles;

    @BeforeEach
    void setUp() {
        authData = authTestUtils.createAuthenticatedUser();
        
        // Create test raffles with different start dates
        LocalDateTime now = LocalDateTime.now();
        log.info("Test setup time: {}", now);
        
        testRaffles = List.of(
            createTestRaffle("Past Start Raffle", now.minusMinutes(5)),
            createTestRaffle("Current Start Raffle", now.minusSeconds(1)), 
            createTestRaffle("Future Start Raffle", now.plusMinutes(5))
        );
    }

    private Raffle createTestRaffle(String title, LocalDateTime startDate) {
        Raffle raffle = TestDataBuilder.raffle()
                .association(authData.association())
                .status(PENDING)
                .title(title)
                .description("Test raffle for activation")
                .startDate(startDate)
                .endDate(LocalDateTime.now().plusDays(7))
                .build();
        return rafflesRepository.save(raffle);
    }

    @Nested
    @DisplayName("activatePendingRaffles()")
    class ActivatePendingRafflesTests {

        @Test
        @DisplayName("Should activate raffles with start date in the past")
        void shouldActivateRafflesWithStartDateInThePast() {    
            // Act
            scheduler.activatePendingRaffles();

            // Assert
            List<Raffle> updatedRaffles = rafflesRepository.findAll();
                
            // Past start raffle should be activated
            Raffle pastStartRaffle = findRaffleByTitle(updatedRaffles, "Past Start Raffle");
            assertThat(pastStartRaffle.getStatus()).isEqualTo(ACTIVE);
            
            // Current start raffle should be activated
            Raffle currentStartRaffle = findRaffleByTitle(updatedRaffles, "Current Start Raffle");
            assertThat(currentStartRaffle.getStatus()).isEqualTo(ACTIVE);
            
            // Future start raffle should remain pending
            Raffle futureStartRaffle = findRaffleByTitle(updatedRaffles, "Future Start Raffle");
            assertThat(futureStartRaffle.getStatus()).isEqualTo(PENDING);
        }

        @Test
        @DisplayName("Should not activate already active raffles")
        void shouldNotActivateAlreadyActiveRaffles() {
            // Arrange - Set one raffle to ACTIVE
            Raffle pastStartRaffle = findRaffleByTitle(testRaffles, "Past Start Raffle");
            pastStartRaffle.setStatus(ACTIVE);
            rafflesRepository.save(pastStartRaffle);

            // Act
            scheduler.activatePendingRaffles();

            // Assert
            List<Raffle> updatedRaffles = rafflesRepository.findAll();
            
            // Already active raffle should remain active
            Raffle activeRaffle = findRaffleByTitle(updatedRaffles, "Past Start Raffle");
            assertThat(activeRaffle.getStatus()).isEqualTo(ACTIVE);

            // Other eligible raffles should be activated
            Raffle currentStartRaffle = findRaffleByTitle(updatedRaffles, "Current Start Raffle");
            assertThat(currentStartRaffle.getStatus()).isEqualTo(ACTIVE);
            
            // Future start raffle should remain pending
            Raffle futureStartRaffle = findRaffleByTitle(updatedRaffles, "Future Start Raffle");
            assertThat(futureStartRaffle.getStatus()).isEqualTo(PENDING);
        }

        @Test
        @DisplayName("Should handle empty list of pending raffles")
        void shouldHandleEmptyListOfPendingRaffles() {
            // Arrange - Delete all test raffles
            rafflesRepository.deleteAll();

            // Act
            scheduler.activatePendingRaffles();

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