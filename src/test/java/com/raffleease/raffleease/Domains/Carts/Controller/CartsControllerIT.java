package com.raffleease.raffleease.Domains.Carts.Controller;

import com.raffleease.raffleease.Base.AbstractIntegrationTest;
import com.raffleease.raffleease.Domains.Carts.Model.Cart;
import com.raffleease.raffleease.Domains.Carts.Repository.CartsRepository;
import com.raffleease.raffleease.Domains.Raffles.Model.Raffle;
import com.raffleease.raffleease.Domains.Raffles.Model.RaffleStatistics;
import com.raffleease.raffleease.Domains.Raffles.Model.RaffleStatus;
import com.raffleease.raffleease.Domains.Raffles.Repository.RafflesRepository;
import com.raffleease.raffleease.Domains.Tickets.Model.Ticket;
import com.raffleease.raffleease.Domains.Tickets.Repository.TicketsRepository;
import com.raffleease.raffleease.util.AuthTestUtils;
import com.raffleease.raffleease.util.AuthTestUtils.AuthTestData;
import com.raffleease.raffleease.util.TestDataBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.ResultActions;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static com.raffleease.raffleease.Domains.Carts.Model.CartStatus.ACTIVE;
import static com.raffleease.raffleease.Domains.Carts.Model.CartStatus.CLOSED;
import static com.raffleease.raffleease.Domains.Tickets.Model.TicketStatus.AVAILABLE;
import static com.raffleease.raffleease.Domains.Tickets.Model.TicketStatus.RESERVED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("Admin Carts Controller Integration Tests")
class CartsControllerIT extends AbstractIntegrationTest {
    @Autowired
    private AuthTestUtils authTestUtils;

    @Autowired
    private CartsRepository cartsRepository;

    @Autowired
    private RafflesRepository rafflesRepository;

    @Autowired
    private TicketsRepository ticketsRepository;

    private AuthTestData authData;
    private String baseEndpoint;
    private Raffle testRaffle;
    private Cart existingCart;
    private List<Ticket> reservedTickets;
    private RaffleStatistics initialStatistics;

    @BeforeEach
    void setUp() {
        authData = authTestUtils.createAuthenticatedUser();
        baseEndpoint = "/v1/associations/" + authData.association().getId() + "/carts";

        // Create a test raffle with statistics
        initialStatistics = TestDataBuilder.statistics()
                .availableTickets(7L) 
                .participants(1L) 
                .ticketsPerParticipant(BigDecimal.valueOf(3)) 
                .build();

        testRaffle = TestDataBuilder.raffle()
                .association(authData.association())
                .status(RaffleStatus.ACTIVE)
                .title("Test Raffle for Cart")
                .totalTickets(10L)
                .statistics(initialStatistics)
                .build();
        testRaffle = rafflesRepository.save(testRaffle);

        // Create some test tickets
        reservedTickets = new ArrayList<>();
        for (int i = 1; i <= 3; i++) {
            Ticket ticket = Ticket.builder()
                    .ticketNumber(String.valueOf(i))
                    .status(RESERVED)
                    .raffle(testRaffle)
                    .build();
            reservedTickets.add(ticket);
        }
        reservedTickets = ticketsRepository.saveAll(reservedTickets);

        // Create an existing active cart with reserved tickets
        existingCart = Cart.builder()
                .status(ACTIVE)
                .user(authData.user())
                .tickets(reservedTickets)
                .build();
        existingCart = cartsRepository.save(existingCart);
    }

    @Nested
    @DisplayName("POST /v1/associations/{associationId}/carts")
    class CreateCartTests {

        @Test
        @DisplayName("Should create new cart and close existing active cart")
        void shouldCreateNewCartAndCloseExistingActiveCart() throws Exception {
            // Act
            ResultActions result = mockMvc.perform(post(baseEndpoint)
                    .with(user(authData.user().getEmail())));

            // Assert
            result.andExpect(status().isCreated())
                    .andExpect(content().contentType("application/json"))
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("New cart created successfully"))
                    .andExpect(jsonPath("$.data.id").exists())
                    .andExpect(jsonPath("$.data.status").value(ACTIVE.name()));

            // Verify existing cart was closed
            Cart closedCart = cartsRepository.findById(existingCart.getId()).orElseThrow();
            assertThat(closedCart.getStatus()).isEqualTo(CLOSED);
            assertThat(closedCart.getTickets()).isNull();

            // Verify tickets were released
            List<Ticket> releasedTickets = ticketsRepository.findAllById(
                    reservedTickets.stream().map(Ticket::getId).toList()
            );
            assertThat(releasedTickets).allSatisfy(ticket -> {
                assertThat(ticket.getStatus()).isEqualTo(AVAILABLE);
                assertThat(ticket.getCart()).isNull();
            });

            // Verify new cart was created
            Cart newCart = cartsRepository.findByUserAndStatus(authData.user(), ACTIVE).orElseThrow();
            assertThat(newCart.getStatus()).isEqualTo(ACTIVE);
            assertThat(newCart.getTickets()).isEmpty();

            // Verify raffle statistics were updated
            Raffle updatedRaffle = rafflesRepository.findById(testRaffle.getId()).orElseThrow();
            RaffleStatistics updatedStats = updatedRaffle.getStatistics();
            assertThat(updatedStats.getAvailableTickets()).isEqualTo(testRaffle.getTotalTickets());
            assertThat(updatedStats.getParticipants()).isEqualTo(0L);
            assertThat(updatedStats.getTicketsPerParticipant()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("Should create new cart when no active cart exists")
        void shouldCreateNewCartWhenNoActiveCartExists() throws Exception {
            // Arrange - Close existing cart
            existingCart.setStatus(CLOSED);
            existingCart.setTickets(null);
            cartsRepository.save(existingCart);

            // Act
            ResultActions result = mockMvc.perform(post(baseEndpoint)
                    .with(user(authData.user().getEmail())));

            // Assert
            result.andExpect(status().isCreated())
                    .andExpect(content().contentType("application/json"))
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("New cart created successfully"))
                    .andExpect(jsonPath("$.data.id").exists())
                    .andExpect(jsonPath("$.data.status").value(ACTIVE.name()));

            // Verify new cart was created
            Cart newCart = cartsRepository.findByUserAndStatus(authData.user(), ACTIVE).orElseThrow();
            assertThat(newCart.getStatus()).isEqualTo(ACTIVE);
            assertThat(newCart.getTickets()).isEmpty();

            // Verify statistics remained unchanged since no tickets were released
            Raffle updatedRaffle = rafflesRepository.findById(testRaffle.getId()).orElseThrow();
            RaffleStatistics updatedStats = updatedRaffle.getStatistics();
            assertThat(updatedStats.getAvailableTickets()).isEqualTo(initialStatistics.getAvailableTickets());
            assertThat(updatedStats.getParticipants()).isEqualTo(initialStatistics.getParticipants());
            assertThat(updatedStats.getTicketsPerParticipant()).isEqualByComparingTo(initialStatistics.getTicketsPerParticipant());
        }
    }
} 