package com.raffleease.raffleease.Domains.Carts.Controller;

import com.raffleease.raffleease.Base.AbstractIntegrationTest;
import com.raffleease.raffleease.Domains.Carts.Model.Cart;
import com.raffleease.raffleease.Domains.Carts.Repository.CartsRepository;
import com.raffleease.raffleease.Domains.Raffles.Model.Raffle;
import com.raffleease.raffleease.Domains.Raffles.Model.RaffleStatus;
import com.raffleease.raffleease.Domains.Tickets.Model.Ticket;
import com.raffleease.raffleease.Domains.Tickets.Repository.TicketsRepository;
import com.raffleease.raffleease.util.AuthTestUtils;
import com.raffleease.raffleease.util.AuthTestUtils.AuthTestData;
import com.raffleease.raffleease.util.TestDataBuilder;
import jakarta.persistence.EntityManager;
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
import static com.raffleease.raffleease.Domains.Tickets.Model.TicketStatus.RESERVED;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("Get Active Cart Controller Integration Tests")
class GetActiveCartControllerIT extends AbstractIntegrationTest {

    @Autowired
    private AuthTestUtils authTestUtils;

    @Autowired
    private CartsRepository cartsRepository;

    @Autowired
    private TicketsRepository ticketsRepository;

    @Autowired
    private EntityManager entityManager;

    private AuthTestData authData;
    private String baseEndpoint;
    private Raffle testRaffle;
    private Cart testCart;
    private List<Ticket> reservedTickets;

    @BeforeEach
    void setUp() {
        authData = authTestUtils.createAuthenticatedUser();
        baseEndpoint = "/v1/associations/" + authData.association().getId() + "/carts/active";

        // Create and save a test raffle
        testRaffle = TestDataBuilder.raffle()
                .association(authData.association())
                .status(RaffleStatus.ACTIVE)
                .title("Test Raffle for Active Cart")
                .totalTickets(10L)
                .ticketPrice(BigDecimal.TEN)
                .build();
        entityManager.persist(testRaffle);
        entityManager.flush();

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
    }

    @Nested
    @DisplayName("GET /v1/associations/{associationId}/carts/active")
    class GetActiveCartTests {

        @Test
        @DisplayName("Should successfully get active cart")
        void shouldSuccessfullyGetActiveCart() throws Exception {
            // Act
            ResultActions result = mockMvc.perform(get(baseEndpoint)
                    .with(user(authData.user().getEmail())));

            // Assert
            result.andExpect(status().isOk())
                    .andExpect(content().contentType("application/json"))
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Active user cart retrieved successfully"))
                    .andExpect(jsonPath("$.data.id").value(testCart.getId()))
                    .andExpect(jsonPath("$.data.status").value(ACTIVE.name()))
                    .andExpect(jsonPath("$.data.tickets").isArray())
                    .andExpect(jsonPath("$.data.tickets.length()").value(3))
                    .andExpect(jsonPath("$.data.tickets[0].id").exists())
                    .andExpect(jsonPath("$.data.tickets[0].ticketNumber").exists())
                    .andExpect(jsonPath("$.data.tickets[0].status").value(RESERVED.name()))
                    .andExpect(jsonPath("$.data.tickets[0].raffleId").value(testRaffle.getId()));
        }

        @Test
        @DisplayName("Should return 404 when no active cart exists")
        void shouldReturn404WhenNoActiveCartExists() throws Exception {
            // Arrange - Close the active cart
            testCart.setStatus(CLOSED);
            testCart.setTickets(null);
            cartsRepository.save(testCart);

            // Act
            ResultActions result = mockMvc.perform(get(baseEndpoint)
                    .with(user(authData.user().getEmail())));

            // Assert
            result.andExpect(status().isNotFound())
                    .andExpect(content().contentType("application/json"))
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("Active cart not found for user with id <" + authData.user().getId() + ">"));
        }

        @Test
        @DisplayName("Should return 403 when user is not authenticated")
        void shouldReturn403WhenUserIsNotAuthenticated() throws Exception {
            // Act
            ResultActions result = mockMvc.perform(get(baseEndpoint));

            // Assert
            result.andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Should return 403 when user is not associated with the association")
        void shouldReturn403WhenUserIsNotAssociated() throws Exception {
            // Arrange - Create another user
            AuthTestData otherAuthData = authTestUtils.createAuthenticatedUserWithCredentials(
                    "otheruser", "other@example.com", "password123");

            // Act
            ResultActions result = mockMvc.perform(get(baseEndpoint)
                    .with(user(otherAuthData.user().getEmail())));

            // Assert
            result.andExpect(status().isForbidden());
        }
    }
} 