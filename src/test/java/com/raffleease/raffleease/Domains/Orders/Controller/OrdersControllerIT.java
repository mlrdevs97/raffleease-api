package com.raffleease.raffleease.Domains.Orders.Controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.raffleease.raffleease.Base.AbstractIntegrationTest;
import com.raffleease.raffleease.Common.Models.PhoneNumberDTO;
import com.raffleease.raffleease.Domains.Carts.Model.Cart;
import com.raffleease.raffleease.Domains.Carts.Repository.CartsRepository;
import com.raffleease.raffleease.Domains.Customers.DTO.CustomerCreate;
import com.raffleease.raffleease.Domains.Images.Model.Image;
import com.raffleease.raffleease.Domains.Images.Model.ImageStatus;
import com.raffleease.raffleease.Domains.Orders.DTOs.OrderCreate;
import com.raffleease.raffleease.Domains.Orders.Model.Order;
import com.raffleease.raffleease.Domains.Orders.Repository.OrdersRepository;
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
import static com.raffleease.raffleease.Domains.Orders.Model.OrderStatus.PENDING;
import static com.raffleease.raffleease.Domains.Raffles.Model.RaffleStatus.PAUSED;
import static com.raffleease.raffleease.Domains.Tickets.Model.TicketStatus.AVAILABLE;
import static com.raffleease.raffleease.Domains.Tickets.Model.TicketStatus.RESERVED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("Admin Orders Controller Integration Tests")
class OrdersControllerIT extends AbstractIntegrationTest {

    @Autowired
    private AuthTestUtils authTestUtils;

    @Autowired
    private CartsRepository cartsRepository;

    @Autowired
    private RafflesRepository rafflesRepository;

    @Autowired
    private TicketsRepository ticketsRepository;

    @Autowired
    private OrdersRepository ordersRepository;

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
        baseEndpoint = "/v1/associations/" + authData.association().getId() + "/orders";

        // Create a test raffle with statistics
        initialStatistics = TestDataBuilder.statistics()
                .availableTickets(7L)
                .participants(1L)
                .ticketsPerParticipant(BigDecimal.valueOf(3))
                .build();

        // Create mock images for the raffle
        List<Image> raffleImages = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            Image image = TestDataBuilder.image()
                    .fileName("raffle-image-" + (i + 1) + ".jpg")
                    .user(authData.user())
                    .association(authData.association())
                    .status(ImageStatus.ACTIVE)
                    .imageOrder(i + 1)
                    .build();
            raffleImages.add(image);
        }

        testRaffle = TestDataBuilder.raffle()
                .association(authData.association())
                .status(RaffleStatus.ACTIVE)
                .title("Test Raffle for Orders")
                .totalTickets(10L)
                .ticketPrice(BigDecimal.TEN)
                .statistics(initialStatistics)
                .images(raffleImages)
                .build();
        testRaffle = rafflesRepository.save(testRaffle);

        // Create an active cart
        testCart = Cart.builder()
                .status(ACTIVE)
                .user(authData.user())
                .tickets(new ArrayList<>())
                .build();
        testCart = cartsRepository.save(testCart);

        // Create reserved tickets and associate them with the cart
        reservedTickets = new ArrayList<>();
        for (int i = 1; i <= 3; i++) {
            Ticket ticket = Ticket.builder()
                    .ticketNumber(String.valueOf(i))
                    .status(RESERVED)
                    .raffle(testRaffle)
                    .cart(testCart)
                    .build();
            reservedTickets.add(ticket);
        }
        reservedTickets = ticketsRepository.saveAll(reservedTickets);

        // Update cart with tickets
        testCart.setTickets(reservedTickets);
        testCart = cartsRepository.save(testCart);
    }

    @Nested
    @DisplayName("POST /v1/associations/{associationId}/orders")
    class CreateOrderTests {

        @Test
        @DisplayName("Should successfully create order with valid data")
        void shouldSuccessfullyCreateOrder() throws Exception {
            // Arrange
            PhoneNumberDTO phoneNumberDTO = new PhoneNumberDTO("+1", "234567890");
            CustomerCreate customerCreate = new CustomerCreate(
                    "John Doe",
                    "john@example.com",
                    phoneNumberDTO
            );

            OrderCreate orderCreate = new OrderCreate(
                    testCart.getId(),
                    testRaffle.getId(),
                    reservedTickets.stream().map(Ticket::getId).toList(),
                    customerCreate,
                    "Test order comment"
            );

            // Act
            ResultActions result = mockMvc.perform(post(baseEndpoint)
                    .with(user(authData.user().getEmail()))
                    .contentType(APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(orderCreate)));

            // Assert
            result.andExpect(status().isCreated())
                    .andExpect(content().contentType("application/json"))
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("New order created successfully"))
                    .andExpect(jsonPath("$.data.id").exists())
                    .andExpect(jsonPath("$.data.status").value(PENDING.name()))
                    .andExpect(jsonPath("$.data.orderReference").exists())
                    .andExpect(jsonPath("$.data.customer.fullName").value("john doe"))
                    .andExpect(jsonPath("$.data.customer.email").value("john@example.com"))
                    .andExpect(jsonPath("$.data.customer.phoneNumber.prefix").value("+1"))
                    .andExpect(jsonPath("$.data.customer.phoneNumber.nationalNumber").value("234567890"))
                    .andExpect(jsonPath("$.data.comment").value("Test order comment"));

            // Verify order was created
            Order createdOrder = ordersRepository.findAll().get(0);
            assertThat(createdOrder.getStatus()).isEqualTo(PENDING);
            assertThat(createdOrder.getOrderReference()).startsWith("ORD-");
            assertThat(createdOrder.getCustomer().getFullName()).isEqualTo("john doe");
            assertThat(createdOrder.getCustomer().getEmail()).isEqualTo("john@example.com");
            assertThat(createdOrder.getComment()).isEqualTo("Test order comment");

            // Verify cart was closed
            Cart updatedCart = cartsRepository.findById(testCart.getId()).orElseThrow();
            assertThat(updatedCart.getStatus()).isEqualTo(CLOSED);
            assertThat(updatedCart.getTickets()).isNull();

            // Verify tickets were transferred to customer
            List<Ticket> updatedTickets = ticketsRepository.findAllById(
                    reservedTickets.stream().map(Ticket::getId).toList()
            );
            assertThat(updatedTickets).allSatisfy(ticket -> {
                assertThat(ticket.getCustomer()).isNotNull();
                assertThat(ticket.getCustomer().getFullName()).isEqualTo("john doe");
                assertThat(ticket.getCart()).isNull();
            });

            // Verify raffle statistics were updated
            Raffle updatedRaffle = rafflesRepository.findById(testRaffle.getId()).orElseThrow();
            RaffleStatistics updatedStats = updatedRaffle.getStatistics();
            assertThat(updatedStats.getTotalOrders()).isEqualTo(1L);
            assertThat(updatedStats.getPendingOrders()).isEqualTo(1L);
        }

        @Test
        @DisplayName("Should fail when cart is closed")
        void shouldFailWhenCartIsClosed() throws Exception {
            // Arrange - Close the cart
            testCart.setStatus(CLOSED);
            testCart = cartsRepository.save(testCart);

            // Release tickets
            reservedTickets.forEach(ticket -> {
                ticket.setStatus(AVAILABLE);
                ticket.setCart(null);
                ticket.setCustomer(null);
            });
            ticketsRepository.saveAll(reservedTickets);

            CustomerCreate customerCreate = new CustomerCreate(
                    "John Doe",
                    "john@example.com",
                    null
            );

            OrderCreate orderCreate = new OrderCreate(
                    testCart.getId(),
                    testRaffle.getId(),
                    reservedTickets.stream().map(Ticket::getId).toList(),
                    customerCreate,
                    null
            );

            // Act
            ResultActions result = mockMvc.perform(post(baseEndpoint)
                    .with(user(authData.user().getEmail()))
                    .contentType(APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(orderCreate)));

            // Assert
            result.andExpect(status().isBadRequest())
                    .andExpect(content().contentType("application/json"))
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("Cannot create order for a closed cart"));

            // Verify no order was created
            assertThat(ordersRepository.findAll()).isEmpty();

            // Verify cart remained closed
            Cart updatedCart = cartsRepository.findById(testCart.getId()).orElseThrow();
            assertThat(updatedCart.getStatus()).isEqualTo(CLOSED);

            // Verify tickets remained unchanged
            List<Long> ticketIds = reservedTickets.stream().map(Ticket::getId).toList();
            List<Ticket> updatedTickets = ticketsRepository.findAllById(ticketIds);
            assertThat(updatedTickets).allSatisfy(ticket -> {
                assertThat(ticket.getCustomer()).isNull();
                assertThat(ticket.getCart()).isNull();
            });

            // Verify statistics were not changed
            Raffle updatedRaffle = rafflesRepository.findById(testRaffle.getId()).orElseThrow();
            assertThat(updatedRaffle.getStatistics().getTotalOrders()).isEqualTo(0L);
            assertThat(updatedRaffle.getStatistics().getPendingOrders()).isEqualTo(0L);
        }

        @Test
        @DisplayName("Should fail when raffle is not active")
        void shouldFailWhenRaffleIsNotActive() throws Exception {
            // Arrange - Set raffle to non-active status
            testRaffle.setStatus(PAUSED);
            testRaffle = rafflesRepository.save(testRaffle);

            CustomerCreate customerCreate = new CustomerCreate(
                    "John Doe",
                    "john@example.com",
                    null
            );

            OrderCreate orderCreate = new OrderCreate(
                    testCart.getId(),
                    testRaffle.getId(),
                    reservedTickets.stream().map(Ticket::getId).toList(),
                    customerCreate,
                    null
            );

            // Act
            ResultActions result = mockMvc.perform(post(baseEndpoint)
                    .with(user(authData.user().getEmail()))
                    .contentType(APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(orderCreate)));

            // Assert
            result.andExpect(status().isBadRequest())
                    .andExpect(content().contentType("application/json"))
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("Cannot create order for PAUSED raffle"));

            // Verify no order was created
            assertThat(ordersRepository.findAll()).isEmpty();

            // Verify cart remained active
            Cart updatedCart = cartsRepository.findById(testCart.getId()).orElseThrow();
            assertThat(updatedCart.getStatus()).isEqualTo(ACTIVE);

            // Verify tickets remained unchanged
            List<Ticket> updatedTickets = ticketsRepository.findAllById(
                    reservedTickets.stream().map(Ticket::getId).toList()
            );
            assertThat(updatedTickets).allSatisfy(ticket -> {
                assertThat(ticket.getCustomer()).isNull();
                assertThat(ticket.getCart().getId()).isEqualTo(testCart.getId());
            });

            // Verify statistics were not changed
            Raffle updatedRaffle = rafflesRepository.findById(testRaffle.getId()).orElseThrow();
            assertThat(updatedRaffle.getStatistics().getTotalOrders()).isEqualTo(0L);
            assertThat(updatedRaffle.getStatistics().getPendingOrders()).isEqualTo(0L);
        }

        @Test
        @DisplayName("Should fail when tickets do not belong to cart")
        void shouldFailWhenTicketsDoNotBelongToCart() throws Exception {
            // Arrange - Create a new ticket not in the cart
            Ticket newTicket = Ticket.builder()
                    .ticketNumber("4")
                    .status(RESERVED)
                    .raffle(testRaffle)
                    .build();
            newTicket = ticketsRepository.save(newTicket);

            CustomerCreate customerCreate = new CustomerCreate(
                    "John Doe",
                    "john@example.com",
                    null
            );

            OrderCreate orderCreate = new OrderCreate(
                    testCart.getId(),
                    testRaffle.getId(),
                    List.of(newTicket.getId()),
                    customerCreate,
                    null
            );

            // Act
            ResultActions result = mockMvc.perform(post(baseEndpoint)
                    .with(user(authData.user().getEmail()))
                    .contentType(APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(orderCreate)));

            // Assert
            result.andExpect(status().isBadRequest())
                    .andExpect(content().contentType("application/json"))
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("Some tickets do not belong to current cart"));

            // Verify no order was created
            assertThat(ordersRepository.findAll()).isEmpty();

            // Verify cart remained active
            Cart updatedCart = cartsRepository.findById(testCart.getId()).orElseThrow();
            assertThat(updatedCart.getStatus()).isEqualTo(ACTIVE);

            // Verify tickets remained unchanged
            List<Ticket> updatedTickets = ticketsRepository.findAllById(
                    reservedTickets.stream().map(Ticket::getId).toList()
            );
            assertThat(updatedTickets).allSatisfy(ticket -> {
                assertThat(ticket.getCustomer()).isNull();
                assertThat(ticket.getCart().getId()).isEqualTo(testCart.getId());
            });

            // Verify statistics were not changed
            Raffle updatedRaffle = rafflesRepository.findById(testRaffle.getId()).orElseThrow();
            assertThat(updatedRaffle.getStatistics().getTotalOrders()).isEqualTo(0L);
            assertThat(updatedRaffle.getStatistics().getPendingOrders()).isEqualTo(0L);
        }

        @Test
        @DisplayName("Should fail when not all cart tickets are included")
        void shouldFailWhenNotAllCartTicketsAreIncluded() throws Exception {
            // Arrange
            CustomerCreate customerCreate = new CustomerCreate(
                    "John Doe",
                    "john@example.com",
                    null
            );

            OrderCreate orderCreate = new OrderCreate(
                    testCart.getId(),
                    testRaffle.getId(),
                    reservedTickets.stream().limit(2).map(Ticket::getId).toList(),
                    customerCreate,
                    null
            );

            // Act
            ResultActions result = mockMvc.perform(post(baseEndpoint)
                    .with(user(authData.user().getEmail()))
                    .contentType(APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(orderCreate)));

            // Assert
            result.andExpect(status().isBadRequest())
                    .andExpect(content().contentType("application/json"))
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("Some tickets in cart are not included in order request"));

            // Verify no order was created
            assertThat(ordersRepository.findAll()).isEmpty();

            // Verify cart remained active
            Cart updatedCart = cartsRepository.findById(testCart.getId()).orElseThrow();
            assertThat(updatedCart.getStatus()).isEqualTo(ACTIVE);

            // Verify tickets remained unchanged
            List<Ticket> updatedTickets = ticketsRepository.findAllById(
                    reservedTickets.stream().map(Ticket::getId).toList()
            );
            assertThat(updatedTickets).allSatisfy(ticket -> {
                assertThat(ticket.getCustomer()).isNull();
                assertThat(ticket.getCart().getId()).isEqualTo(testCart.getId());
            });

            // Verify statistics were not changed
            Raffle updatedRaffle = rafflesRepository.findById(testRaffle.getId()).orElseThrow();
            assertThat(updatedRaffle.getStatistics().getTotalOrders()).isEqualTo(0L);
            assertThat(updatedRaffle.getStatistics().getPendingOrders()).isEqualTo(0L);
        }
    }
} 