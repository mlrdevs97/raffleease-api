package com.raffleease.raffleease.Domains.Carts.Controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.raffleease.raffleease.Base.AbstractIntegrationTest;
import com.raffleease.raffleease.Domains.Carts.DTO.ReservationRequest;
import com.raffleease.raffleease.Domains.Carts.Model.Cart;
import com.raffleease.raffleease.Domains.Carts.Model.CartStatus;
import com.raffleease.raffleease.Domains.Carts.Repository.CartsRepository;
import com.raffleease.raffleease.Domains.Raffles.Model.Raffle;
import com.raffleease.raffleease.Domains.Raffles.Model.RaffleStatistics;
import com.raffleease.raffleease.Domains.Raffles.Model.RaffleStatus;
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
import java.util.ArrayList;
import java.util.List;

import static com.raffleease.raffleease.Domains.Carts.Model.CartStatus.ACTIVE;
import static com.raffleease.raffleease.Domains.Tickets.Model.TicketStatus.AVAILABLE;
import static com.raffleease.raffleease.Domains.Tickets.Model.TicketStatus.RESERVED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("Reservations Controller Integration Tests")
class ReservationsControllerIT extends AbstractIntegrationTest {
    @Autowired
    private AuthTestUtils authTestUtils;

    @Autowired
    private CartsRepository cartsRepository;

    @Autowired
    private RafflesRepository rafflesRepository;

    @Autowired
    private TicketsRepository ticketsRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private AuthTestData authData;
    private String baseEndpoint;
    private Raffle testRaffle;
    private Cart testCart;
    private List<Ticket> availableTickets;
    private RaffleStatistics initialStatistics;

    @BeforeEach
    void setUp() {
        authData = authTestUtils.createAuthenticatedUser();
        
        // Create a test raffle with statistics
        initialStatistics = TestDataBuilder.statistics()
                .availableTickets(10L)
                .participants(0L)
                .ticketsPerParticipant(BigDecimal.ZERO)
                .build();
        
        testRaffle = TestDataBuilder.raffle()
                .association(authData.association())
                .status(RaffleStatus.ACTIVE)
                .title("Test Raffle for Reservations")
                .totalTickets(10L)
                .ticketPrice(BigDecimal.TEN)
                .statistics(initialStatistics)
                .build();
        testRaffle = rafflesRepository.save(testRaffle);

        // Create available tickets
        availableTickets = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            Ticket ticket = Ticket.builder()
                    .ticketNumber(String.valueOf(i))
                    .status(AVAILABLE)
                    .raffle(testRaffle)
                    .build();
            availableTickets.add(ticket);
        }
        availableTickets = ticketsRepository.saveAll(availableTickets);

        // Create an active cart
        testCart = Cart.builder()
                .status(ACTIVE)
                .user(authData.user())
                .tickets(new ArrayList<>())
                .build();
        testCart = cartsRepository.save(testCart);

        baseEndpoint = "/v1/associations/" + authData.association().getId() + "/carts/" + testCart.getId() + "/reservations";
    }

    @Nested
    @DisplayName("POST /v1/associations/{associationId}/carts/{cartId}/reservations")
    class ReserveTicketsTests {

        @Test
        @DisplayName("Should successfully reserve tickets")
        void shouldSuccessfullyReserveTickets() throws Exception {
            // Arrange
            List<Long> ticketIds = availableTickets.stream()
                    .limit(3)
                    .map(Ticket::getId)
                    .toList();
            ReservationRequest request = new ReservationRequest(ticketIds);

            // Act
            ResultActions result = mockMvc.perform(post(baseEndpoint)
                    .with(user(authData.user().getEmail()))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)));

            // Assert
            result.andExpect(status().isOk())
                    .andExpect(content().contentType("application/json"))
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("New reservation generated successfully"))
                    .andExpect(jsonPath("$.data.id").value(testCart.getId()))
                    .andExpect(jsonPath("$.data.status").value(ACTIVE.name()))
                    .andExpect(jsonPath("$.data.tickets").isArray())
                    .andExpect(jsonPath("$.data.tickets.length()").value(3));

            // Verify tickets were reserved
            List<Ticket> reservedTickets = ticketsRepository.findAllById(ticketIds);
            assertThat(reservedTickets).allSatisfy(ticket -> {
                assertThat(ticket.getStatus()).isEqualTo(RESERVED);
                assertThat(ticket.getCart().getId()).isEqualTo(testCart.getId());
            });

            // Verify cart was updated
            Cart updatedCart = cartsRepository.findById(testCart.getId()).orElseThrow();
            assertThat(updatedCart.getTickets()).hasSize(3);
            assertThat(updatedCart.getTickets()).allSatisfy(ticket -> 
                assertThat(ticket.getStatus()).isEqualTo(RESERVED)
            );

            // Verify raffle statistics were updated
            Raffle updatedRaffle = rafflesRepository.findById(testRaffle.getId()).orElseThrow();
            RaffleStatistics updatedStats = updatedRaffle.getStatistics();
            assertThat(updatedStats.getAvailableTickets()).isEqualTo(7L);
            assertThat(updatedStats.getParticipants()).isEqualTo(1L);
            assertThat(updatedStats.getTicketsPerParticipant()).isEqualByComparingTo(BigDecimal.valueOf(3));
        }

        @Test
        @DisplayName("Should fail when trying to reserve already reserved tickets")
        void shouldFailWhenReservingReservedTickets() throws Exception {
            // Arrange - Reserve some tickets first
            List<Ticket> alreadyReserved = availableTickets.subList(0, 2);
            alreadyReserved.forEach(ticket -> {
                ticket.setStatus(RESERVED);
                ticket.setCart(testCart);
            });
            ticketsRepository.saveAll(alreadyReserved);

            // Try to reserve the same tickets again
            List<Long> ticketIds = alreadyReserved.stream()
                    .map(Ticket::getId)
                    .toList();
            ReservationRequest request = new ReservationRequest(ticketIds);

            // Act
            ResultActions result = mockMvc.perform(post(baseEndpoint)
                    .with(user(authData.user().getEmail()))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)));

            // Assert
            result.andExpect(status().isBadRequest())
                    .andExpect(content().contentType("application/json"))
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("Some tickets are not available"));

            // Verify tickets remained reserved
            List<Ticket> tickets = ticketsRepository.findAllById(ticketIds);
            assertThat(tickets).allSatisfy(ticket -> {
                assertThat(ticket.getStatus()).isEqualTo(RESERVED);
                assertThat(ticket.getCart().getId()).isEqualTo(testCart.getId());
            });

            // Verify statistics were not changed
            Raffle updatedRaffle = rafflesRepository.findById(testRaffle.getId()).orElseThrow();
            assertThat(updatedRaffle.getStatistics().getAvailableTickets())
                    .isEqualTo(initialStatistics.getAvailableTickets());
        }

        @Test
        @DisplayName("Should fail when trying to reserve tickets from different association")
        void shouldFailWhenReservingTicketsFromDifferentAssociation() throws Exception {
            // Arrange - Create another association's raffle and tickets
            AuthTestData otherAuthData = authTestUtils.createAuthenticatedUserWithCredentials(
                    "otheruser", "other@example.com", "password123");
            Raffle otherRaffle = TestDataBuilder.raffle()
                    .association(otherAuthData.association())
                    .status(RaffleStatus.ACTIVE)
                    .title("Other Association Raffle")
                    .totalTickets(5L)
                    .ticketPrice(BigDecimal.TEN)
                    .statistics(TestDataBuilder.statistics()
                            .availableTickets(5L)
                            .participants(0L)
                            .ticketsPerParticipant(BigDecimal.ZERO)
                            .build())
                    .build();
            otherRaffle = rafflesRepository.save(otherRaffle);

            Ticket otherTicket = Ticket.builder()
                    .ticketNumber("1")
                    .status(AVAILABLE)
                    .raffle(otherRaffle)
                    .build();
            otherTicket = ticketsRepository.save(otherTicket);

            // Try to reserve the other association's ticket
            ReservationRequest request = new ReservationRequest(List.of(otherTicket.getId()));

            // Act
            ResultActions result = mockMvc.perform(post(baseEndpoint)
                    .with(user(authData.user().getEmail()))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)));

            // Assert
            result.andExpect(status().isBadRequest())
                    .andExpect(content().contentType("application/json"))
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("Some tickets do not belong to an association raffle"));

            // Verify ticket remained available
            Ticket updatedTicket = ticketsRepository.findById(otherTicket.getId()).orElseThrow();
            assertThat(updatedTicket.getStatus()).isEqualTo(AVAILABLE);
            assertThat(updatedTicket.getCart()).isNull();

            // Verify statistics were not changed
            Raffle updatedRaffle = rafflesRepository.findById(testRaffle.getId()).orElseThrow();
            assertThat(updatedRaffle.getStatistics().getAvailableTickets())
                    .isEqualTo(initialStatistics.getAvailableTickets());
        }

        @Test
        @DisplayName("Should fail when trying to reserve tickets in a closed cart")
        void shouldFailWhenReservingTicketsInClosedCart() throws Exception {
            // Arrange - Close the cart
            testCart.setStatus(CartStatus.CLOSED);
            testCart = cartsRepository.save(testCart);

            // Try to reserve tickets
            List<Long> ticketIds = availableTickets.stream()
                    .limit(2)
                    .map(Ticket::getId)
                    .toList();
            ReservationRequest request = new ReservationRequest(ticketIds);

            // Act
            ResultActions result = mockMvc.perform(post(baseEndpoint)
                    .with(user(authData.user().getEmail()))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)));

            // Assert
            result.andExpect(status().isBadRequest())
                    .andExpect(content().contentType("application/json"))
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("Cart must be ACTIVE to reserve or release tickets"));

            // Verify tickets remained available
            List<Ticket> tickets = ticketsRepository.findAllById(ticketIds);
            assertThat(tickets).allSatisfy(ticket -> {
                assertThat(ticket.getStatus()).isEqualTo(AVAILABLE);
                assertThat(ticket.getCart()).isNull();
            });

            // Verify cart remained closed
            Cart updatedCart = cartsRepository.findById(testCart.getId()).orElseThrow();
            assertThat(updatedCart.getStatus()).isEqualTo(CartStatus.CLOSED);
            assertThat(updatedCart.getTickets()).isEmpty();

            // Verify statistics were not changed
            Raffle updatedRaffle = rafflesRepository.findById(testRaffle.getId()).orElseThrow();
            assertThat(updatedRaffle.getStatistics().getAvailableTickets())
                    .isEqualTo(initialStatistics.getAvailableTickets());
        }

        @Test
        @DisplayName("Should fail when trying to reserve tickets in another user's cart")
        void shouldFailWhenReservingTicketsInAnotherUsersCart() throws Exception {
            // Arrange - Create another user and try to use first user's cart
            AuthTestData otherAuthData = authTestUtils.createAuthenticatedUserWithCredentials(
                    "otheruser", "other@example.com", "password123");

            List<Long> ticketIds = availableTickets.stream()
                    .limit(2)
                    .map(Ticket::getId)
                    .toList();
            ReservationRequest request = new ReservationRequest(ticketIds);

            // Act - Use other user's authentication for first user's cart
            ResultActions result = mockMvc.perform(post(baseEndpoint)
                    .with(user(otherAuthData.user().getEmail()))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)));

            // Assert
            result.andExpect(status().isForbidden())
                    .andExpect(content().contentType("application/json"))
                    .andExpect(jsonPath("$.message").value("You are not allowed to access this association"))
                    .andExpect(jsonPath("$.statusCode").value(403))
                    .andExpect(jsonPath("$.statusText").value("Forbidden"))
                    .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));

            // Verify tickets remained available
            List<Ticket> tickets = ticketsRepository.findAllById(ticketIds);
            assertThat(tickets).allSatisfy(ticket -> {
                assertThat(ticket.getStatus()).isEqualTo(AVAILABLE);
                assertThat(ticket.getCart()).isNull();
            });

            // Verify cart remained unchanged
            Cart updatedCart = cartsRepository.findById(testCart.getId()).orElseThrow();
            assertThat(updatedCart.getStatus()).isEqualTo(ACTIVE);
            assertThat(updatedCart.getTickets()).isEmpty();

            // Verify statistics were not changed
            Raffle updatedRaffle = rafflesRepository.findById(testRaffle.getId()).orElseThrow();
            assertThat(updatedRaffle.getStatistics().getAvailableTickets())
                    .isEqualTo(initialStatistics.getAvailableTickets());
        }
    }
} 