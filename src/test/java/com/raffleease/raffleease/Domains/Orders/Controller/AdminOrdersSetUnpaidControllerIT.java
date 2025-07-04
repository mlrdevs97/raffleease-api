package com.raffleease.raffleease.Domains.Orders.Controller;

import com.raffleease.raffleease.Base.AbstractIntegrationTest;
import com.raffleease.raffleease.Domains.Associations.Model.AssociationRole;
import com.raffleease.raffleease.Domains.Customers.Model.Customer;
import com.raffleease.raffleease.Domains.Customers.Repository.CustomersRepository;
import com.raffleease.raffleease.Domains.Orders.Model.Order;
import com.raffleease.raffleease.Domains.Orders.Model.OrderItem;
import com.raffleease.raffleease.Domains.Orders.Model.OrderStatus;
import com.raffleease.raffleease.Domains.Orders.Repository.OrdersRepository;
import com.raffleease.raffleease.Domains.Payments.Model.Payment;
import com.raffleease.raffleease.Domains.Payments.Repository.PaymentsRepository;
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
import org.springframework.test.web.servlet.ResultActions;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static com.raffleease.raffleease.Domains.Orders.Model.OrderStatus.*;
import static com.raffleease.raffleease.Domains.Orders.Model.OrderStatus.PENDING;
import static com.raffleease.raffleease.Domains.Payments.Model.PaymentMethod.CARD;
import static com.raffleease.raffleease.Domains.Raffles.Model.RaffleStatus.*;
import static com.raffleease.raffleease.Domains.Tickets.Model.TicketStatus.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("Admin Orders Set Unpaid Controller Integration Tests")
class AdminOrdersSetUnpaidControllerIT extends AbstractIntegrationTest {

    @Autowired
    private AuthTestUtils authTestUtils;

    @Autowired
    private OrdersRepository ordersRepository;

    @Autowired
    private RafflesRepository rafflesRepository;

    @Autowired
    private CustomersRepository customersRepository;

    @Autowired
    private PaymentsRepository paymentsRepository;

    @Autowired
    private TicketsRepository ticketsRepository;

    private AuthTestData authData;
    private String baseEndpoint;
    private Raffle testRaffle;
    private RaffleStatistics initialStatistics;

    @BeforeEach
    void setUp() {
        authData = authTestUtils.createAuthenticatedUser();
        baseEndpoint = "/v1/associations/" + authData.association().getId() + "/orders";
        setupTestRaffle();
    }

    private void setupTestRaffle() {
        // Create raffle statistics with some pending orders
        initialStatistics = TestDataBuilder.statistics()
                .availableTickets(2L)
                .soldTickets(0L)
                .revenue(BigDecimal.ZERO)
                .participants(1L)
                .totalOrders(2L)
                .pendingOrders(2L)
                .completedOrders(0L)
                .cancelledOrders(0L)
                .unpaidOrders(0L)
                .refundedOrders(0L)
                .averageOrderValue(BigDecimal.ZERO)
                .build();

        // Create test raffle with COMPLETED status (required for setUnpaid)
        testRaffle = TestDataBuilder.raffle()
                .association(authData.association())
                .status(RaffleStatus.COMPLETED)
                .title("Test Raffle for Set Unpaid")
                .ticketPrice(BigDecimal.valueOf(25.00))
                .totalTickets(5L)
                .firstTicketNumber(1L)
                .endDate(LocalDateTime.now().minusDays(1)) // Past end date
                .statistics(initialStatistics)
                .build();
        testRaffle = rafflesRepository.save(testRaffle);

        // Create tickets for the raffle
        List<Ticket> tickets = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            Ticket ticket = Ticket.builder()
                    .ticketNumber(String.valueOf(i))
                    .status(AVAILABLE)
                    .raffle(testRaffle)
                    .build();
            tickets.add(ticket);
        }
        tickets = ticketsRepository.saveAll(tickets);
        testRaffle.setTickets(tickets);
    }

    private Order createTestOrder(OrderStatus status, List<Ticket> orderTickets, BigDecimal total) {
        // Create customer
        Customer customer = customersRepository.save(TestDataBuilder.customer()
                .fullName("jane doe")
                .email("jane@example.com")
                .phoneNumber("+1", "987654321")
                .build());

        // Create order
        Order order = Order.builder()
                .raffle(testRaffle)
                .customer(customer)
                .status(status)
                .orderReference("ORD-UNPAID-" + System.currentTimeMillis())
                .comment("Test set unpaid order")
                .orderItems(new ArrayList<>())
                .build();
        order = ordersRepository.save(order);

        // Create payment
        Payment payment = Payment.builder()
                .order(order)
                .total(total)
                .paymentMethod(CARD)
                .build();
        payment = paymentsRepository.save(payment);
        order.setPayment(payment);

        // Create order items
        List<OrderItem> orderItems = new ArrayList<>();
        for (Ticket ticket : orderTickets) {
            OrderItem item = OrderItem.builder()
                    .order(order)
                    .ticketNumber(ticket.getTicketNumber())
                    .priceAtPurchase(testRaffle.getTicketPrice())
                    .ticketId(ticket.getId())
                    .raffleId(testRaffle.getId())
                    .customerId(customer.getId())
                    .build();
            orderItems.add(item);
        }
        order.setOrderItems(orderItems);

        // Reserve tickets for pending orders
        if (status == PENDING) {
            Customer finalCustomer = customer;
            orderTickets.forEach(ticket -> {
                ticket.setStatus(RESERVED);
                ticket.setCustomer(finalCustomer);
            });
            ticketsRepository.saveAll(orderTickets);
        }

        return ordersRepository.save(order);
    }

    @Nested
    @DisplayName("PUT /v1/associations/{associationId}/orders/{orderId}/unpaid - Authorization Tests")
    class AuthorizationTests {

        @Test
        @DisplayName("Should return 403 when COLLABORATOR tries to set order as unpaid")
        void shouldReturn403WhenCollaboratorTriesToSetOrderAsUnpaid() throws Exception {
            // Arrange
            AuthTestData collaboratorData = authTestUtils.createAuthenticatedUserInSameAssociation(
                    authData.association(), AssociationRole.COLLABORATOR);
            
            List<Ticket> orderTickets = new ArrayList<>(testRaffle.getTickets().subList(0, 1));
            Order testOrder = createTestOrder(PENDING, orderTickets, BigDecimal.valueOf(25.00));
            
            String endpoint = baseEndpoint + "/" + testOrder.getId() + "/unpaid";

            // Act
            ResultActions result = mockMvc.perform(put(endpoint)
                    .with(user(collaboratorData.user().getEmail())));

            // Assert
            result.andExpect(status().isForbidden())
                    .andExpect(content().contentType("application/json"))
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("Only administrators and members can set orders as unpaid"));

            // Verify order was not changed
            Order unchangedOrder = ordersRepository.findById(testOrder.getId()).orElseThrow();
            assertThat(unchangedOrder.getStatus()).isEqualTo(PENDING);
            assertThat(unchangedOrder.getUnpaidAt()).isNull();
        }

        @Test
        @DisplayName("Should successfully set order as unpaid for ADMIN role")
        void shouldSuccessfullySetOrderAsUnpaidForAdmin() throws Exception {
            // Arrange
            AuthTestData adminData = authTestUtils.createAuthenticatedUserInSameAssociation(
                    authData.association(), AssociationRole.ADMIN);
            
            List<Ticket> orderTickets = new ArrayList<>(testRaffle.getTickets().subList(0, 1));
            Order testOrder = createTestOrder(PENDING, orderTickets, BigDecimal.valueOf(25.00));
            
            String endpoint = baseEndpoint + "/" + testOrder.getId() + "/unpaid";

            // Act
            ResultActions result = mockMvc.perform(put(endpoint)
                    .with(user(adminData.user().getEmail())));

            // Assert
            result.andExpect(status().isOk())
                    .andExpect(content().contentType("application/json"))
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Order unpaid successfully"))
                    .andExpect(jsonPath("$.data.status").value("UNPAID"));

            // Verify order was set as unpaid
            Order unpaidOrder = ordersRepository.findById(testOrder.getId()).orElseThrow();
            assertThat(unpaidOrder.getStatus()).isEqualTo(UNPAID);
            assertThat(unpaidOrder.getUnpaidAt()).isNotNull();
        }

        @Test
        @DisplayName("Should successfully set order as unpaid for MEMBER role")
        void shouldSuccessfullySetOrderAsUnpaidForMember() throws Exception {
            // Arrange
            AuthTestData memberData = authTestUtils.createAuthenticatedUserInSameAssociation(
                    authData.association(), AssociationRole.MEMBER);
            
            List<Ticket> orderTickets = new ArrayList<>(testRaffle.getTickets().subList(0, 1));
            Order testOrder = createTestOrder(PENDING, orderTickets, BigDecimal.valueOf(25.00));
            
            String endpoint = baseEndpoint + "/" + testOrder.getId() + "/unpaid";

            // Act
            ResultActions result = mockMvc.perform(put(endpoint)
                    .with(user(memberData.user().getEmail())));

            // Assert
            result.andExpect(status().isOk())
                    .andExpect(content().contentType("application/json"))
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Order unpaid successfully"))
                    .andExpect(jsonPath("$.data.status").value("UNPAID"));

            // Verify order was set as unpaid
            Order unpaidOrder = ordersRepository.findById(testOrder.getId()).orElseThrow();
            assertThat(unpaidOrder.getStatus()).isEqualTo(UNPAID);
            assertThat(unpaidOrder.getUnpaidAt()).isNotNull();
        }

        @Test
        @DisplayName("Should return 401 when user is not authenticated")
        void shouldReturn401WhenNotAuthenticated() throws Exception {
            // Arrange
            List<Ticket> orderTickets = new ArrayList<>(testRaffle.getTickets().subList(0, 1));
            Order testOrder = createTestOrder(PENDING, orderTickets, BigDecimal.valueOf(25.00));
            
            String endpoint = baseEndpoint + "/" + testOrder.getId() + "/unpaid";

            // Act
            ResultActions result = mockMvc.perform(put(endpoint));

            // Assert
            result.andExpect(status().isUnauthorized());

            // Verify order was not changed
            Order unchangedOrder = ordersRepository.findById(testOrder.getId()).orElseThrow();
            assertThat(unchangedOrder.getStatus()).isEqualTo(PENDING);
            assertThat(unchangedOrder.getUnpaidAt()).isNull();
        }
    }

    @Nested
    @DisplayName("PUT /v1/associations/{associationId}/orders/{orderId}/unpaid - Successful Set Unpaid")
    class SuccessfulSetUnpaidTests {

        @Test
        @DisplayName("Should successfully set pending order to unpaid")
        void shouldSuccessfullySetPendingOrderToUnpaid() throws Exception {
            // Arrange
            List<Ticket> orderTickets = new ArrayList<>(testRaffle.getTickets().subList(0, 2)); // 2 tickets
            Order testOrder = createTestOrder(PENDING, orderTickets, BigDecimal.valueOf(50.00));
            
            String endpoint = baseEndpoint + "/" + testOrder.getId() + "/unpaid";

            // Act
            ResultActions result = mockMvc.perform(put(endpoint)
                    .with(user(authData.user().getEmail())));

            // Assert
            result.andExpect(status().isOk())
                    .andExpect(content().contentType("application/json"))
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Order unpaid successfully"))
                    .andExpect(jsonPath("$.data.id").value(testOrder.getId()))
                    .andExpect(jsonPath("$.data.status").value("UNPAID"));
                    // Note: unpaidAt is not included in OrderDTO, so we don't check it in JSON response

            // Verify order was updated
            Order updatedOrder = ordersRepository.findById(testOrder.getId()).orElseThrow();
            assertThat(updatedOrder.getStatus()).isEqualTo(UNPAID);
            assertThat(updatedOrder.getUnpaidAt()).isNotNull();

            // Verify tickets were released (back to AVAILABLE, customer cleared)
            List<Long> ticketIds = orderTickets.stream().map(Ticket::getId).toList();
            List<Ticket> updatedTickets = ticketsRepository.findAllById(ticketIds);
            assertThat(updatedTickets).allSatisfy(ticket -> {
                assertThat(ticket.getStatus()).isEqualTo(AVAILABLE);
                assertThat(ticket.getCustomer()).isNull();
                assertThat(ticket.getCart()).isNull();
            });

            // Verify raffle statistics were updated
            Raffle updatedRaffle = rafflesRepository.findById(testRaffle.getId()).orElseThrow();
            RaffleStatistics stats = updatedRaffle.getStatistics();
            assertThat(stats.getPendingOrders()).isEqualTo(1L); // decreased by 1 (from 2 to 1)
            assertThat(stats.getUnpaidOrders()).isEqualTo(1L); // increased by 1 (from 0 to 1)
            assertThat(stats.getAvailableTickets()).isEqualTo(4L); // increased by 2 (from 2 to 4)

            // Verify raffle remains completed
            assertThat(updatedRaffle.getStatus()).isEqualTo(RaffleStatus.COMPLETED);
        }

        @Test
        @DisplayName("Should handle set unpaid with different ticket quantities")
        void shouldHandleSetUnpaidWithDifferentTicketQuantities() throws Exception {
            // Test setting unpaid orders with different numbers of tickets
            int[] ticketCounts = {1, 3};
            
            for (int ticketCount : ticketCounts) {
                // Reset raffle state
                setupTestRaffle();
                
                // Arrange
                List<Ticket> orderTickets = new ArrayList<>(testRaffle.getTickets().subList(0, ticketCount));
                Order testOrder = createTestOrder(PENDING, orderTickets, BigDecimal.valueOf(25.00 * ticketCount));
                
                String endpoint = baseEndpoint + "/" + testOrder.getId() + "/unpaid";

                // Act
                ResultActions result = mockMvc.perform(put(endpoint)
                        .with(user(authData.user().getEmail())));

                // Assert
                result.andExpect(status().isOk())
                        .andExpect(jsonPath("$.success").value(true))
                        .andExpect(jsonPath("$.data.status").value("UNPAID"));

                // Verify tickets were released
                List<Long> ticketIds = orderTickets.stream().map(Ticket::getId).toList();
                List<Ticket> updatedTickets = ticketsRepository.findAllById(ticketIds);
                assertThat(updatedTickets).allSatisfy(ticket -> 
                    assertThat(ticket.getStatus()).isEqualTo(AVAILABLE)
                );

                // Verify statistics updated correctly
                Raffle updatedRaffle = rafflesRepository.findById(testRaffle.getId()).orElseThrow();
                RaffleStatistics stats = updatedRaffle.getStatistics();
                assertThat(stats.getUnpaidOrders()).isEqualTo(1L);
                assertThat(stats.getPendingOrders()).isEqualTo(1L); // 2 - 1 = 1 pending order left
                assertThat(stats.getAvailableTickets()).isEqualTo(2L + ticketCount); // original 2 plus released tickets
            }
        }

        @Test
        @DisplayName("Should handle multiple order set unpaid and update statistics correctly")
        void shouldHandleMultipleOrderSetUnpaidAndUpdateStatisticsCorrectly() throws Exception {
            // Arrange - Create and set unpaid first order
            List<Ticket> firstOrderTickets = new ArrayList<>(testRaffle.getTickets().subList(0, 1));
            Order firstOrder = createTestOrder(PENDING, firstOrderTickets, BigDecimal.valueOf(25.00));
            
            // Set unpaid first order
            mockMvc.perform(put(baseEndpoint + "/" + firstOrder.getId() + "/unpaid")
                    .with(user(authData.user().getEmail())));

            // Create second order
            List<Ticket> secondOrderTickets = new ArrayList<>(testRaffle.getTickets().subList(1, 3));
            Order secondOrder = createTestOrder(PENDING, secondOrderTickets, BigDecimal.valueOf(50.00));
            
            String endpoint = baseEndpoint + "/" + secondOrder.getId() + "/unpaid";

            // Act - Set unpaid second order
            ResultActions result = mockMvc.perform(put(endpoint)
                    .with(user(authData.user().getEmail())));

            // Assert
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));

            // Verify statistics after second set unpaid
            Raffle updatedRaffle = rafflesRepository.findById(testRaffle.getId()).orElseThrow();
            RaffleStatistics stats = updatedRaffle.getStatistics();
            assertThat(stats.getUnpaidOrders()).isEqualTo(2L); // 2 unpaid orders
            assertThat(stats.getPendingOrders()).isEqualTo(0L); // no pending orders left
            assertThat(stats.getAvailableTickets()).isEqualTo(5L); // all tickets available again
        }
    }

    @Nested
    @DisplayName("PUT /v1/associations/{associationId}/orders/{orderId}/unpaid - Validation Failures")
    class ValidationFailureTests {

        @Test
        @DisplayName("Should fail when trying to set unpaid completed order")
        void shouldFailWhenTryingToSetUnpaidCompletedOrder() throws Exception {
            // Arrange
            List<Ticket> orderTickets = new ArrayList<>(testRaffle.getTickets().subList(0, 1));
            Order testOrder = createTestOrder(OrderStatus.COMPLETED, orderTickets, BigDecimal.valueOf(25.00));
            
            String endpoint = baseEndpoint + "/" + testOrder.getId() + "/unpaid";

            // Act
            ResultActions result = mockMvc.perform(put(endpoint)
                    .with(user(authData.user().getEmail())));

            // Assert
            result.andExpect(status().isBadRequest())
                    .andExpect(content().contentType("application/json"))
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("Unsupported status transition from COMPLETED to UNPAID"));

            // Verify order was not changed
            Order unchangedOrder = ordersRepository.findById(testOrder.getId()).orElseThrow();
            assertThat(unchangedOrder.getStatus()).isEqualTo(OrderStatus.COMPLETED);
            assertThat(unchangedOrder.getUnpaidAt()).isNull();
        }

        @Test
        @DisplayName("Should fail when trying to set unpaid cancelled order")
        void shouldFailWhenTryingToSetUnpaidCancelledOrder() throws Exception {
            // Arrange
            List<Ticket> orderTickets = new ArrayList<>(testRaffle.getTickets().subList(0, 1));
            Order testOrder = createTestOrder(CANCELLED, orderTickets, BigDecimal.valueOf(25.00));
            
            String endpoint = baseEndpoint + "/" + testOrder.getId() + "/unpaid";

            // Act
            ResultActions result = mockMvc.perform(put(endpoint)
                    .with(user(authData.user().getEmail())));

            // Assert
            result.andExpect(status().isBadRequest())
                    .andExpect(content().contentType("application/json"))
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("Unsupported status transition from CANCELLED to UNPAID"));
        }

        @Test
        @DisplayName("Should fail when trying to set unpaid refunded order")
        void shouldFailWhenTryingToSetUnpaidRefundedOrder() throws Exception {
            // Arrange
            List<Ticket> orderTickets = new ArrayList<>(testRaffle.getTickets().subList(0, 1));
            Order testOrder = createTestOrder(REFUNDED, orderTickets, BigDecimal.valueOf(25.00));
            
            String endpoint = baseEndpoint + "/" + testOrder.getId() + "/unpaid";

            // Act
            ResultActions result = mockMvc.perform(put(endpoint)
                    .with(user(authData.user().getEmail())));

            // Assert
            result.andExpect(status().isBadRequest())
                    .andExpect(content().contentType("application/json"))
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("Unsupported status transition from REFUNDED to UNPAID"));
        }

        @Test
        @DisplayName("Should fail when trying to set unpaid already unpaid order")
        void shouldFailWhenTryingToSetUnpaidAlreadyUnpaidOrder() throws Exception {
            // Arrange
            List<Ticket> orderTickets = new ArrayList<>(testRaffle.getTickets().subList(0, 1));
            Order testOrder = createTestOrder(UNPAID, orderTickets, BigDecimal.valueOf(25.00));
            
            String endpoint = baseEndpoint + "/" + testOrder.getId() + "/unpaid";

            // Act
            ResultActions result = mockMvc.perform(put(endpoint)
                    .with(user(authData.user().getEmail())));

            // Assert
            result.andExpect(status().isBadRequest())
                    .andExpect(content().contentType("application/json"))
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("Unsupported status transition from UNPAID to UNPAID"));
        }

        @Test
        @DisplayName("Should fail when order does not exist")
        void shouldFailWhenOrderDoesNotExist() throws Exception {
            // Arrange
            Long nonExistentOrderId = 99999L;
            String endpoint = baseEndpoint + "/" + nonExistentOrderId + "/unpaid";

            // Act
            ResultActions result = mockMvc.perform(put(endpoint)
                    .with(user(authData.user().getEmail())));

            // Assert
            result.andExpect(status().isNotFound())
                    .andExpect(content().contentType("application/json"))
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("Should fail when raffle is not completed")
        void shouldFailWhenRaffleIsNotCompleted() throws Exception {
            // Arrange - Set raffle to ACTIVE status (invalid for setUnpaid)
            testRaffle.setStatus(ACTIVE);
            rafflesRepository.save(testRaffle);
            
            List<Ticket> orderTickets = new ArrayList<>(testRaffle.getTickets().subList(0, 1));
            Order testOrder = createTestOrder(PENDING, orderTickets, BigDecimal.valueOf(25.00));
            
            String endpoint = baseEndpoint + "/" + testOrder.getId() + "/unpaid";

            // Act
            ResultActions result = mockMvc.perform(put(endpoint)
                    .with(user(authData.user().getEmail())));

            // Assert
            result.andExpect(status().isBadRequest())
                    .andExpect(content().contentType("application/json"))
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("Cannot transition order from PENDING to UNPAID when raffle status is ACTIVE, expected COMPLETED"));

            // Verify order was not changed
            Order unchangedOrder = ordersRepository.findById(testOrder.getId()).orElseThrow();
            assertThat(unchangedOrder.getStatus()).isEqualTo(PENDING);
            assertThat(unchangedOrder.getUnpaidAt()).isNull();
        }

        @Test
        @DisplayName("Should fail when raffle is pending")
        void shouldFailWhenRaffleIsPending() throws Exception {
            // Arrange - Set raffle to PENDING status (invalid for setUnpaid)
            testRaffle.setStatus(RaffleStatus.PENDING);
            rafflesRepository.save(testRaffle);
            
            List<Ticket> orderTickets = new ArrayList<>(testRaffle.getTickets().subList(0, 1));
            Order testOrder = createTestOrder(PENDING, orderTickets, BigDecimal.valueOf(25.00));
            
            String endpoint = baseEndpoint + "/" + testOrder.getId() + "/unpaid";

            // Act
            ResultActions result = mockMvc.perform(put(endpoint)
                    .with(user(authData.user().getEmail())));

            // Assert
            result.andExpect(status().isBadRequest())
                    .andExpect(content().contentType("application/json"))
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("Cannot transition order from PENDING to UNPAID when raffle status is PENDING, expected COMPLETED"));
        }

        @Test
        @DisplayName("Should fail when raffle is paused")
        void shouldFailWhenRaffleIsPaused() throws Exception {
            // Arrange - Set raffle to PAUSED status (invalid for setUnpaid)
            testRaffle.setStatus(PAUSED);
            rafflesRepository.save(testRaffle);
            
            List<Ticket> orderTickets = new ArrayList<>(testRaffle.getTickets().subList(0, 1));
            Order testOrder = createTestOrder(PENDING, orderTickets, BigDecimal.valueOf(25.00));
            
            String endpoint = baseEndpoint + "/" + testOrder.getId() + "/unpaid";

            // Act
            ResultActions result = mockMvc.perform(put(endpoint)
                    .with(user(authData.user().getEmail())));

            // Assert
            result.andExpect(status().isBadRequest())
                    .andExpect(content().contentType("application/json"))
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("Cannot transition order from PENDING to UNPAID when raffle status is PAUSED, expected COMPLETED"));
        }
    }

    @Nested
    @DisplayName("PUT /v1/associations/{associationId}/orders/{orderId}/unpaid - Edge Cases")
    class EdgeCaseTests {

        @Test
        @DisplayName("Should fail when order has no order items")
        void shouldFailWhenOrderHasNoOrderItems() throws Exception {
            // Arrange - Create order without items (edge case)
            Customer customer = customersRepository.save(TestDataBuilder.customer()
                    .fullName("test customer")
                    .email("test@example.com")
                    .phoneNumber("+1", "234567890")
                    .build());

            Order testOrder = Order.builder()
                    .raffle(testRaffle)
                    .customer(customer)
                    .status(PENDING)
                    .orderReference("ORD-EMPTY-" + System.currentTimeMillis())
                    .orderItems(new ArrayList<>())
                    .build();
            testOrder = ordersRepository.save(testOrder);

            // Create payment
            Payment payment = Payment.builder()
                    .order(testOrder)
                    .total(BigDecimal.ZERO)
                    .paymentMethod(CARD)
                    .build();
            payment = paymentsRepository.save(payment);
            testOrder.setPayment(payment);
            testOrder = ordersRepository.save(testOrder);

            String endpoint = baseEndpoint + "/" + testOrder.getId() + "/unpaid";

            // Act
            ResultActions result = mockMvc.perform(put(endpoint)
                    .with(user(authData.user().getEmail())));

            // Assert - Should fail with NotFoundException because TicketsQueryServiceImpl.findAllById() 
            // throws exception when called with empty list
            result.andExpect(status().isNotFound())
                    .andExpect(content().contentType("application/json"))
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("Some tickets were not found"));

            // Verify order was not changed
            Order unchangedOrder = ordersRepository.findById(testOrder.getId()).orElseThrow();
            assertThat(unchangedOrder.getStatus()).isEqualTo(PENDING);
            assertThat(unchangedOrder.getUnpaidAt()).isNull();
        }

        @Test
        @DisplayName("Should fail when order items reference non-existent tickets")
        void shouldFailWhenOrderItemsReferenceNonExistentTickets() throws Exception {
            // Arrange - Create order with items that reference non-existent tickets
            Customer customer = customersRepository.save(TestDataBuilder.customer()
                    .fullName("test customer")
                    .email("test@example.com")
                    .phoneNumber("+1", "234567890")
                    .build());

            Order testOrder = Order.builder()
                    .raffle(testRaffle)
                    .customer(customer)
                    .status(PENDING)
                    .orderReference("ORD-INVALID-" + System.currentTimeMillis())
                    .orderItems(new ArrayList<>())
                    .build();
            testOrder = ordersRepository.save(testOrder);

            // Create payment
            Payment payment = Payment.builder()
                    .order(testOrder)
                    .total(BigDecimal.valueOf(25.00))
                    .paymentMethod(CARD)
                    .build();
            payment = paymentsRepository.save(payment);
            testOrder.setPayment(payment);

            // Create order item with non-existent ticket ID
            OrderItem invalidItem = OrderItem.builder()
                    .order(testOrder)
                    .ticketNumber("999")
                    .priceAtPurchase(testRaffle.getTicketPrice())
                    .ticketId(99999L) // Non-existent ticket ID
                    .raffleId(testRaffle.getId())
                    .customerId(customer.getId())
                    .build();
            
            List<OrderItem> orderItems = new ArrayList<>();
            orderItems.add(invalidItem);
            testOrder.setOrderItems(orderItems);
            testOrder = ordersRepository.save(testOrder);

            String endpoint = baseEndpoint + "/" + testOrder.getId() + "/unpaid";

            // Act
            ResultActions result = mockMvc.perform(put(endpoint)
                    .with(user(authData.user().getEmail())));

            // Assert - Should fail with NotFoundException
            result.andExpect(status().isNotFound())
                    .andExpect(content().contentType("application/json"))
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("Some tickets were not found"));

            // Verify order was not changed
            Order unchangedOrder = ordersRepository.findById(testOrder.getId()).orElseThrow();
            assertThat(unchangedOrder.getStatus()).isEqualTo(PENDING);
            assertThat(unchangedOrder.getUnpaidAt()).isNull();
        }
    }
} 