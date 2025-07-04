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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("Release Tickets Controller Integration Tests")
class ReleaseControllerIT extends AbstractIntegrationTest {

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
    private List<Ticket> reservedTickets;
    private RaffleStatistics initialStatistics;

    @BeforeEach
    void setUp() {
        authData = authTestUtils.createAuthenticatedUser();
        
        // Create a test raffle with statistics
        initialStatistics = TestDataBuilder.statistics()
                .availableTickets(7L)
                .participants(1L)
                .ticketsPerParticipant(BigDecimal.valueOf(3))
                .build();
        
        testRaffle = TestDataBuilder.raffle()
                .association(authData.association())
                .status(RaffleStatus.ACTIVE)
                .title("Test Raffle for Release")
                .totalTickets(10L)
                .ticketPrice(BigDecimal.TEN)
                .statistics(initialStatistics)
                .build();
        testRaffle = rafflesRepository.save(testRaffle);

        // Create reserved tickets
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

        // Create an active cart with reserved tickets
        testCart = Cart.builder()
                .status(ACTIVE)
                .user(authData.user())
                .tickets(reservedTickets)
                .build();
        testCart = cartsRepository.save(testCart);

        baseEndpoint = "/v1/associations/" + authData.association().getId() + "/carts/" + testCart.getId() + "/reservations";
    }

    @Nested
    @DisplayName("PUT /v1/associations/{associationId}/carts/{cartId}/reservations")
    class ReleaseTicketsTests {

        @Test
        @DisplayName("Should successfully release tickets")
        void shouldSuccessfullyReleaseTickets() throws Exception {
            // Arrange
            List<Long> ticketIds = reservedTickets.stream()
                    .limit(2)
                    .map(Ticket::getId)
                    .toList();
            ReservationRequest request = new ReservationRequest(ticketIds);

            // Act
            ResultActions result = mockMvc.perform(put(baseEndpoint)
                    .with(user(authData.user().getEmail()))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)));

            // Assert
            result.andExpect(status().isOk())
                    .andExpect(content().contentType("application/json"))
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Tickets released successfully"));

            // Verify tickets were released
            List<Ticket> releasedTickets = ticketsRepository.findAllById(ticketIds);
            assertThat(releasedTickets).allSatisfy(ticket -> {
                assertThat(ticket.getStatus()).isEqualTo(AVAILABLE);
                assertThat(ticket.getCart()).isNull();
            });

            // Verify cart was updated
            Cart updatedCart = cartsRepository.findById(testCart.getId()).orElseThrow();
            assertThat(updatedCart.getTickets()).hasSize(1);
            assertThat(updatedCart.getTickets().get(0).getStatus()).isEqualTo(RESERVED);

            // Verify raffle statistics were updated
            Raffle updatedRaffle = rafflesRepository.findById(testRaffle.getId()).orElseThrow();
            RaffleStatistics updatedStats = updatedRaffle.getStatistics();
            assertThat(updatedStats.getAvailableTickets()).isEqualTo(9L);
            assertThat(updatedStats.getParticipants()).isEqualTo(0L);
            assertThat(updatedStats.getTicketsPerParticipant()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("Should fail when trying to release tickets not in cart")
        void shouldFailWhenReleasingTicketsNotInCart() throws Exception {
            // Arrange - Create a new available ticket not in the cart
            Ticket newTicket = Ticket.builder()
                    .ticketNumber("4")
                    .status(AVAILABLE)
                    .raffle(testRaffle)
                    .build();
            newTicket = ticketsRepository.save(newTicket);

            // Try to release the new ticket
            ReservationRequest request = new ReservationRequest(List.of(newTicket.getId()));

            // Act
            ResultActions result = mockMvc.perform(put(baseEndpoint)
                    .with(user(authData.user().getEmail()))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)));

            // Assert
            result.andExpect(status().isBadRequest())
                    .andExpect(content().contentType("application/json"))
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("Cannot release tickets that do not belong to the cart. Invalid ticket IDs: [" + newTicket.getId() + "]"));

            // Verify ticket remained available
            Ticket updatedTicket = ticketsRepository.findById(newTicket.getId()).orElseThrow();
            assertThat(updatedTicket.getStatus()).isEqualTo(AVAILABLE);
            assertThat(updatedTicket.getCart()).isNull();

            // Verify cart remained unchanged
            Cart updatedCart = cartsRepository.findById(testCart.getId()).orElseThrow();
            assertThat(updatedCart.getTickets()).hasSize(3);
            assertThat(updatedCart.getTickets()).allSatisfy(ticket -> 
                assertThat(ticket.getStatus()).isEqualTo(RESERVED)
            );

            // Verify statistics were not changed
            Raffle updatedRaffle = rafflesRepository.findById(testRaffle.getId()).orElseThrow();
            assertThat(updatedRaffle.getStatistics().getAvailableTickets())
                    .isEqualTo(initialStatistics.getAvailableTickets());
        }

        @Test
        @DisplayName("Should fail when trying to release tickets from different association")
        void shouldFailWhenReleasingTicketsFromDifferentAssociation() throws Exception {
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
                    .status(RESERVED)
                    .raffle(otherRaffle)
                    .build();
            otherTicket = ticketsRepository.save(otherTicket);

            // Try to release the other association's ticket
            ReservationRequest request = new ReservationRequest(List.of(otherTicket.getId()));

            // Act
            ResultActions result = mockMvc.perform(put(baseEndpoint)
                    .with(user(authData.user().getEmail()))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)));

            // Assert
            result.andExpect(status().isBadRequest())
                    .andExpect(content().contentType("application/json"))
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("Some tickets do not belong to an association raffle"));

            // Verify ticket remained reserved
            Ticket updatedTicket = ticketsRepository.findById(otherTicket.getId()).orElseThrow();
            assertThat(updatedTicket.getStatus()).isEqualTo(RESERVED);
            assertThat(updatedTicket.getCart()).isNull();

            // Verify cart remained unchanged
            Cart updatedCart = cartsRepository.findById(testCart.getId()).orElseThrow();
            assertThat(updatedCart.getTickets()).hasSize(3);
            assertThat(updatedCart.getTickets()).allSatisfy(ticket -> 
                assertThat(ticket.getStatus()).isEqualTo(RESERVED)
            );

            // Verify statistics were not changed
            Raffle updatedRaffle = rafflesRepository.findById(testRaffle.getId()).orElseThrow();
            assertThat(updatedRaffle.getStatistics().getAvailableTickets())
                    .isEqualTo(initialStatistics.getAvailableTickets());
        }

        @Test
        @DisplayName("Should fail when trying to release tickets from a closed cart")
        void shouldFailWhenReleasingTicketsFromClosedCart() throws Exception {
            // Arrange - Close the cart
            testCart.setStatus(CartStatus.CLOSED);
            testCart = cartsRepository.save(testCart);

            // Try to release tickets
            List<Long> ticketIds = reservedTickets.stream()
                    .limit(2)
                    .map(Ticket::getId)
                    .toList();
            ReservationRequest request = new ReservationRequest(ticketIds);

            // Act
            ResultActions result = mockMvc.perform(put(baseEndpoint)
                    .with(user(authData.user().getEmail()))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)));

            // Assert
            result.andExpect(status().isBadRequest())
                    .andExpect(content().contentType("application/json"))
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("Cart must be ACTIVE to reserve or release tickets"));

            // Verify tickets remained reserved
            List<Ticket> tickets = ticketsRepository.findAllById(ticketIds);
            assertThat(tickets).allSatisfy(ticket -> {
                assertThat(ticket.getStatus()).isEqualTo(RESERVED);
                assertThat(ticket.getCart()).isNull();
            });

            // Verify cart remained closed
            Cart updatedCart = cartsRepository.findById(testCart.getId()).orElseThrow();
            assertThat(updatedCart.getStatus()).isEqualTo(CartStatus.CLOSED);
            List<Ticket> cartTickets = ticketsRepository.findAllByCart(updatedCart);
            assertThat(cartTickets).isEmpty();

            // Verify statistics were not changed
            Raffle updatedRaffle = rafflesRepository.findById(testRaffle.getId()).orElseThrow();
            assertThat(updatedRaffle.getStatistics().getAvailableTickets())
                    .isEqualTo(initialStatistics.getAvailableTickets());
        }

        @Test
        @DisplayName("Should fail when trying to release tickets from another user's cart")
        void shouldFailWhenReleasingTicketsFromAnotherUsersCart() throws Exception {
            // Arrange - Create another user in the same association
            AuthTestData otherAuthData = authTestUtils.createAuthenticatedUserInSameAssociation(authData.association());

            reservedTickets.forEach(ticket -> ticket.setCart(testCart));
            reservedTickets = ticketsRepository.saveAll(reservedTickets);

            List<Long> ticketIds = reservedTickets.stream()
                    .limit(2)
                    .map(Ticket::getId)
                    .toList();
            ReservationRequest request = new ReservationRequest(ticketIds);

            // Act - Use other user's authentication for first user's cart
            ResultActions result = mockMvc.perform(put(baseEndpoint)
                    .with(user(otherAuthData.user().getEmail()))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)));

            // Assert
            result.andExpect(status().isForbidden())
                    .andExpect(content().contentType("application/json"))
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("You are not allowed to access this cart"))
                    .andExpect(jsonPath("$.statusCode").value(403))
                    .andExpect(jsonPath("$.statusText").value("Forbidden"))
                    .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));

            // Verify tickets remained reserved in original cart
            List<Ticket> tickets = ticketsRepository.findAllById(ticketIds);
            assertThat(tickets).allSatisfy(ticket -> {
                assertThat(ticket.getStatus()).isEqualTo(RESERVED);
                assertThat(ticket.getCart()).isNotNull();
                assertThat(ticket.getCart().getId()).isEqualTo(testCart.getId());
            });

            // Verify cart remained unchanged
            Cart updatedCart = cartsRepository.findById(testCart.getId()).orElseThrow();
            assertThat(updatedCart.getStatus()).isEqualTo(ACTIVE);
            assertThat(updatedCart.getTickets()).hasSize(3);
            assertThat(updatedCart.getTickets()).allSatisfy(ticket -> 
                assertThat(ticket.getStatus()).isEqualTo(RESERVED)
            );

            // Verify statistics were not changed
            Raffle updatedRaffle = rafflesRepository.findById(testRaffle.getId()).orElseThrow();
            assertThat(updatedRaffle.getStatistics().getAvailableTickets())
                    .isEqualTo(initialStatistics.getAvailableTickets());
        }
    }
}