package com.raffleease.raffleease.Domains.Orders.Controller;

import com.raffleease.raffleease.Base.AbstractIntegrationTest;
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
import com.raffleease.raffleease.Domains.Raffles.Repository.RafflesRepository;
import com.raffleease.raffleease.Domains.Raffles.Services.RafflesStatisticsService;
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

import static com.raffleease.raffleease.Domains.Orders.Model.OrderStatus.*;
import static com.raffleease.raffleease.Domains.Orders.Model.OrderStatus.PENDING;
import static com.raffleease.raffleease.Domains.Payments.Model.PaymentMethod.CARD;
import static com.raffleease.raffleease.Domains.Raffles.Model.RaffleStatus.*;
import static com.raffleease.raffleease.Domains.Raffles.Model.RaffleStatus.COMPLETED;
import static com.raffleease.raffleease.Domains.Tickets.Model.TicketStatus.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("Admin Orders Cancel Controller Integration Tests")
class AdminOrdersCancelControllerIT extends AbstractIntegrationTest {

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

    @Autowired
    private RafflesStatisticsService rafflesStatisticsService;

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
        // Create raffle statistics with clean initial state
        initialStatistics = TestDataBuilder.statistics()
                .availableTickets(5L)
                .build();

        // Create test raffle with 5 tickets
        testRaffle = TestDataBuilder.raffle()
                .association(authData.association())
                .status(ACTIVE)
                .title("Test Raffle for Order Cancellation")
                .ticketPrice(BigDecimal.valueOf(25.00))
                .totalTickets(5L)
                .firstTicketNumber(1L)
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
                .orderReference("ORD-CANCEL-" + System.currentTimeMillis())
                .comment("Test cancellation order")
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

        // Reserve tickets for the order (basic reservation without statistics)
        Customer finalCustomer = customer;
        orderTickets.forEach(ticket -> {
            ticket.setStatus(RESERVED);
            ticket.setCustomer(finalCustomer);
        });
        ticketsRepository.saveAll(orderTickets);

        return ordersRepository.save(order);
    }

    @Nested
    @DisplayName("PUT /v1/associations/{associationId}/orders/{orderId}/cancel - Successful Cancellation")
    class SuccessfulCancellationTests {

        @Test
        @DisplayName("Should successfully cancel a pending order")
        void shouldSuccessfullyCancelPendingOrder() throws Exception {
            // Arrange - Mock statistics state after reservation and order creation
            RaffleStatistics stats = testRaffle.getStatistics();
            stats.setAvailableTickets(3L);  // 5 total - 2 reserved = 3 available
            stats.setParticipants(1L);      // 1 participant made reservations
            stats.setTotalOrders(1L);       // 1 order created
            stats.setPendingOrders(1L);     // 1 pending order
            testRaffle = rafflesRepository.save(testRaffle);
            
            List<Ticket> orderTickets = new ArrayList<>(testRaffle.getTickets().subList(0, 2)); // 2 tickets
            Order testOrder = createTestOrder(PENDING, orderTickets, BigDecimal.valueOf(50.00));
            
            String endpoint = baseEndpoint + "/" + testOrder.getId() + "/cancel";

            // Act
            ResultActions result = mockMvc.perform(put(endpoint)
                    .with(user(authData.user().getEmail())));

            // Assert
            result.andExpect(status().isOk())
                    .andExpect(content().contentType("application/json"))
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Order cancelled successfully"))
                    .andExpect(jsonPath("$.data.id").value(testOrder.getId()))
                    .andExpect(jsonPath("$.data.status").value("CANCELLED"))
                    .andExpect(jsonPath("$.data.cancelledAt").exists());

            // Verify order was updated
            Order updatedOrder = ordersRepository.findById(testOrder.getId()).orElseThrow();
            assertThat(updatedOrder.getStatus()).isEqualTo(OrderStatus.CANCELLED);
            assertThat(updatedOrder.getCancelledAt()).isNotNull();

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
            stats = updatedRaffle.getStatistics();
            assertThat(stats.getPendingOrders()).isEqualTo(0L); // decreased by 1
            assertThat(stats.getCancelledOrders()).isEqualTo(1L); // increased by 1
            assertThat(stats.getAvailableTickets()).isEqualTo(5L); // increased by 2 (back to original 5)

            // Verify raffle remains active
            assertThat(updatedRaffle.getStatus()).isEqualTo(ACTIVE);
        }

        @Test
        @DisplayName("Should handle cancellation with different ticket quantities")
        void shouldHandleCancellationWithDifferentTicketQuantities() throws Exception {
            // Test cancelling orders with different numbers of tickets
            int[] ticketCounts = {1, 3};
            
            for (int ticketCount : ticketCounts) {
                // Arrange
                List<Ticket> orderTickets = new ArrayList<>(testRaffle.getTickets().subList(0, ticketCount));
                Order testOrder = createTestOrder(PENDING, orderTickets, BigDecimal.valueOf(25.00 * ticketCount));
                
                String endpoint = baseEndpoint + "/" + testOrder.getId() + "/cancel";

                // Act
                ResultActions result = mockMvc.perform(put(endpoint)
                        .with(user(authData.user().getEmail())));

                // Assert
                result.andExpect(status().isOk())
                        .andExpect(jsonPath("$.success").value(true))
                        .andExpect(jsonPath("$.data.status").value("CANCELLED"));

                // Verify tickets were released
                List<Long> ticketIds = orderTickets.stream().map(Ticket::getId).toList();
                List<Ticket> updatedTickets = ticketsRepository.findAllById(ticketIds);
                assertThat(updatedTickets).allSatisfy(ticket -> 
                    assertThat(ticket.getStatus()).isEqualTo(AVAILABLE)
                );

                // Reset tickets for next iteration
                orderTickets.forEach(ticket -> ticket.setStatus(AVAILABLE));
                ticketsRepository.saveAll(orderTickets);
            }
        }

        @Test
        @DisplayName("Should handle multiple order cancellations and update statistics correctly")
        void shouldHandleMultipleOrderCancellationsAndUpdateStatisticsCorrectly() throws Exception {
            // Arrange - Create and cancel first order
            // Mock statistics for first order
            RaffleStatistics stats = testRaffle.getStatistics();
            stats.setAvailableTickets(4L);  // 5 total - 1 reserved = 4 available
            stats.setParticipants(1L);      // 1 participant
            stats.setTotalOrders(1L);       // 1 order created
            stats.setPendingOrders(1L);     // 1 pending order
            testRaffle = rafflesRepository.save(testRaffle);
            
            List<Ticket> firstOrderTickets = new ArrayList<>(testRaffle.getTickets().subList(0, 1));
            Order firstOrder = createTestOrder(PENDING, firstOrderTickets, BigDecimal.valueOf(25.00));
            
            // Cancel first order
            mockMvc.perform(put(baseEndpoint + "/" + firstOrder.getId() + "/cancel")
                    .with(user(authData.user().getEmail())));

            // Mock statistics for second order (after first cancellation)
            testRaffle = rafflesRepository.findById(testRaffle.getId()).orElseThrow();
            stats = testRaffle.getStatistics();
            stats.setAvailableTickets(3L);  // 5 total - 2 reserved = 3 available  
            stats.setTotalOrders(2L);       // 2 orders total
            stats.setPendingOrders(1L);     // 1 pending order (first was cancelled)
            testRaffle = rafflesRepository.save(testRaffle);

            // Create second order
            List<Ticket> secondOrderTickets = new ArrayList<>(testRaffle.getTickets().subList(1, 3));
            Order secondOrder = createTestOrder(PENDING, secondOrderTickets, BigDecimal.valueOf(50.00));
            
            String endpoint = baseEndpoint + "/" + secondOrder.getId() + "/cancel";

            // Act - Cancel second order
            ResultActions result = mockMvc.perform(put(endpoint)
                    .with(user(authData.user().getEmail())));

            // Assert
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));

            // Verify statistics after second cancellation
            Raffle updatedRaffle = rafflesRepository.findById(testRaffle.getId()).orElseThrow();
            stats = updatedRaffle.getStatistics();
            assertThat(stats.getCancelledOrders()).isEqualTo(2L); // 2 cancelled orders
            assertThat(stats.getPendingOrders()).isEqualTo(0L); // no pending orders left
            assertThat(stats.getAvailableTickets()).isEqualTo(5L); // all tickets available again
        }
    }

    @Nested
    @DisplayName("PUT /v1/associations/{associationId}/orders/{orderId}/cancel - Validation Failures")
    class ValidationFailureTests {

        @Test
        @DisplayName("Should fail when trying to cancel already completed order")
        void shouldFailWhenTryingToCancelAlreadyCompletedOrder() throws Exception {
            // Arrange
            List<Ticket> orderTickets = new ArrayList<>(testRaffle.getTickets().subList(0, 1));
            Order testOrder = createTestOrder(OrderStatus.COMPLETED, orderTickets, BigDecimal.valueOf(25.00));
            
            String endpoint = baseEndpoint + "/" + testOrder.getId() + "/cancel";

            // Act
            ResultActions result = mockMvc.perform(put(endpoint)
                    .with(user(authData.user().getEmail())));

            // Assert
            result.andExpect(status().isBadRequest())
                    .andExpect(content().contentType("application/json"))
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("Unsupported status transition from COMPLETED to CANCELLED"));

            // Verify order was not changed
            Order unchangedOrder = ordersRepository.findById(testOrder.getId()).orElseThrow();
            assertThat(unchangedOrder.getStatus()).isEqualTo(OrderStatus.COMPLETED);
            assertThat(unchangedOrder.getCancelledAt()).isNull();
        }

        @Test
        @DisplayName("Should fail when trying to cancel already cancelled order")
        void shouldFailWhenTryingToCancelAlreadyCancelledOrder() throws Exception {
            // Arrange
            List<Ticket> orderTickets = new ArrayList<>(testRaffle.getTickets().subList(0, 1));
            Order testOrder = createTestOrder(OrderStatus.CANCELLED, orderTickets, BigDecimal.valueOf(25.00));
            
            String endpoint = baseEndpoint + "/" + testOrder.getId() + "/cancel";

            // Act
            ResultActions result = mockMvc.perform(put(endpoint)
                    .with(user(authData.user().getEmail())));

            // Assert
            result.andExpect(status().isBadRequest())
                    .andExpect(content().contentType("application/json"))
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("Unsupported status transition from CANCELLED to CANCELLED"));
        }

        @Test
        @DisplayName("Should fail when trying to cancel unpaid order")
        void shouldFailWhenTryingToCancelUnpaidOrder() throws Exception {
            // Arrange
            List<Ticket> orderTickets = new ArrayList<>(testRaffle.getTickets().subList(0, 1));
            Order testOrder = createTestOrder(UNPAID, orderTickets, BigDecimal.valueOf(25.00));
            
            String endpoint = baseEndpoint + "/" + testOrder.getId() + "/cancel";

            // Act
            ResultActions result = mockMvc.perform(put(endpoint)
                    .with(user(authData.user().getEmail())));

            // Assert
            result.andExpect(status().isBadRequest())
                    .andExpect(content().contentType("application/json"))
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("Unsupported status transition from UNPAID to CANCELLED"));
        }

        @Test
        @DisplayName("Should fail when trying to cancel refunded order")
        void shouldFailWhenTryingToCancelRefundedOrder() throws Exception {
            // Arrange
            List<Ticket> orderTickets = new ArrayList<>(testRaffle.getTickets().subList(0, 1));
            Order testOrder = createTestOrder(REFUNDED, orderTickets, BigDecimal.valueOf(25.00));
            
            String endpoint = baseEndpoint + "/" + testOrder.getId() + "/cancel";

            // Act
            ResultActions result = mockMvc.perform(put(endpoint)
                    .with(user(authData.user().getEmail())));

            // Assert
            result.andExpect(status().isBadRequest())
                    .andExpect(content().contentType("application/json"))
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("Unsupported status transition from REFUNDED to CANCELLED"));
        }

        @Test
        @DisplayName("Should fail when order does not exist")
        void shouldFailWhenOrderDoesNotExist() throws Exception {
            // Arrange
            Long nonExistentOrderId = 99999L;
            String endpoint = baseEndpoint + "/" + nonExistentOrderId + "/cancel";

            // Act
            ResultActions result = mockMvc.perform(put(endpoint)
                    .with(user(authData.user().getEmail())));

            // Assert
            result.andExpect(status().isNotFound())
                    .andExpect(content().contentType("application/json"))
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("Should fail when raffle is not active")
        void shouldFailWhenRaffleIsNotActive() throws Exception {
            // Arrange - Set raffle to PAUSED status
            testRaffle.setStatus(PAUSED);
            rafflesRepository.save(testRaffle);
            
            List<Ticket> orderTickets = new ArrayList<>(testRaffle.getTickets().subList(0, 1));
            Order testOrder = createTestOrder(PENDING, orderTickets, BigDecimal.valueOf(25.00));
            
            String endpoint = baseEndpoint + "/" + testOrder.getId() + "/cancel";

            // Act
            ResultActions result = mockMvc.perform(put(endpoint)
                    .with(user(authData.user().getEmail())));

            // Assert
            result.andExpect(status().isBadRequest())
                    .andExpect(content().contentType("application/json"))
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("Cannot transition order from PENDING to CANCELLED when raffle status is PAUSED, expected ACTIVE"));

            // Verify order was not changed
            Order unchangedOrder = ordersRepository.findById(testOrder.getId()).orElseThrow();
            assertThat(unchangedOrder.getStatus()).isEqualTo(PENDING);
            assertThat(unchangedOrder.getCancelledAt()).isNull();
        }

        @Test
        @DisplayName("Should fail when raffle is completed")
        void shouldFailWhenRaffleIsCompleted() throws Exception {
            // Arrange - Set raffle to COMPLETED status
            testRaffle.setStatus(COMPLETED);
            rafflesRepository.save(testRaffle);
            
            List<Ticket> orderTickets = new ArrayList<>(testRaffle.getTickets().subList(0, 1));
            Order testOrder = createTestOrder(PENDING, orderTickets, BigDecimal.valueOf(25.00));
            
            String endpoint = baseEndpoint + "/" + testOrder.getId() + "/cancel";

            // Act
            ResultActions result = mockMvc.perform(put(endpoint)
                    .with(user(authData.user().getEmail())));

            // Assert
            result.andExpect(status().isBadRequest())
                    .andExpect(content().contentType("application/json"))
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("Cannot transition order from PENDING to CANCELLED when raffle status is COMPLETED, expected ACTIVE"));
        }
    }

    @Nested
    @DisplayName("PUT /v1/associations/{associationId}/orders/{orderId}/cancel - Edge Cases")
    class EdgeCaseTests {

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

            String endpoint = baseEndpoint + "/" + testOrder.getId() + "/cancel";

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
            assertThat(unchangedOrder.getCancelledAt()).isNull();
        }
    }
} 