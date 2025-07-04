package com.raffleease.raffleease.Domains.Orders.Controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.raffleease.raffleease.Base.AbstractIntegrationTest;
import com.raffleease.raffleease.Domains.Customers.Model.Customer;
import com.raffleease.raffleease.Domains.Customers.Repository.CustomersRepository;
import com.raffleease.raffleease.Domains.Orders.DTOs.OrderComplete;
import com.raffleease.raffleease.Domains.Orders.Model.Order;
import com.raffleease.raffleease.Domains.Orders.Model.OrderItem;
import com.raffleease.raffleease.Domains.Orders.Model.OrderStatus;
import com.raffleease.raffleease.Domains.Orders.Repository.OrdersRepository;
import com.raffleease.raffleease.Domains.Payments.Model.Payment;
import com.raffleease.raffleease.Domains.Payments.Model.PaymentMethod;
import com.raffleease.raffleease.Domains.Payments.Repository.PaymentsRepository;
import com.raffleease.raffleease.Domains.Raffles.Model.CompletionReason;
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
import static com.raffleease.raffleease.Domains.Payments.Model.PaymentMethod.*;
import static com.raffleease.raffleease.Domains.Raffles.Model.CompletionReason.ALL_TICKETS_SOLD;
import static com.raffleease.raffleease.Domains.Raffles.Model.RaffleStatus.ACTIVE;
import static com.raffleease.raffleease.Domains.Raffles.Model.RaffleStatus.COMPLETED;
import static com.raffleease.raffleease.Domains.Tickets.Model.TicketStatus.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("Admin Orders Complete Controller Integration Tests")
class AdminOrdersCompleteControllerIT extends AbstractIntegrationTest {

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
    private ObjectMapper objectMapper;

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
        // Create raffle statistics
        initialStatistics = TestDataBuilder.statistics()
                .availableTickets(2L)
                .soldTickets(0L)
                .revenue(BigDecimal.ZERO)
                .participants(1L)
                .totalOrders(1L)
                .pendingOrders(1L)
                .completedOrders(0L)
                .cancelledOrders(0L)
                .unpaidOrders(0L)
                .refundedOrders(0L)
                .averageOrderValue(BigDecimal.ZERO)
                .firstSaleDate(null)
                .lastSaleDate(null)
                .build();

        // Create test raffle with 5 tickets
        testRaffle = TestDataBuilder.raffle()
                .association(authData.association())
                .status(ACTIVE)
                .title("Test Raffle for Order Completion")
                .ticketPrice(BigDecimal.valueOf(20.00))
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
                .fullName("john doe")
                .email("john@example.com")
                .phoneNumber("+1", "234567890")
                .build());

        // Create order
        Order order = Order.builder()
                .raffle(testRaffle)
                .customer(customer)
                .status(status)
                .orderReference("ORD-TEST-" + System.currentTimeMillis())
                .comment("Test order")
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

        // Reserve tickets for the order
        Customer finalCustomer = customer;
        orderTickets.forEach(ticket -> {
            ticket.setStatus(RESERVED);
            ticket.setCustomer(finalCustomer);
        });
        ticketsRepository.saveAll(orderTickets);

        return ordersRepository.save(order);
    }

    @Nested
    @DisplayName("PUT /v1/associations/{associationId}/orders/{orderId}/complete - Successful Completion")
    class SuccessfulCompletionTests {

        @Test
        @DisplayName("Should successfully complete a pending order")
        void shouldSuccessfullyCompletePendingOrder() throws Exception {
            // Arrange
            List<Ticket> orderTickets = new ArrayList<>(testRaffle.getTickets().subList(0, 2)); // 2 tickets
            Order testOrder = createTestOrder(PENDING, orderTickets, BigDecimal.valueOf(40.00));
            
            OrderComplete completeRequest = OrderComplete.builder()
                    .paymentMethod(PAYPAL)
                    .build();

            String endpoint = baseEndpoint + "/" + testOrder.getId() + "/complete";

            // Act
            ResultActions result = mockMvc.perform(put(endpoint)
                    .with(user(authData.user().getEmail()))
                    .contentType(APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(completeRequest)));

            // Assert
            result.andExpect(status().isOk())
                    .andExpect(content().contentType("application/json"))
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Order completed successfully"))
                    .andExpect(jsonPath("$.data.id").value(testOrder.getId()))
                    .andExpect(jsonPath("$.data.status").value("COMPLETED"))
                    .andExpect(jsonPath("$.data.payment.paymentMethod").value("paypal"))
                    .andExpect(jsonPath("$.data.completedAt").exists());

            // Verify order was updated
            Order updatedOrder = ordersRepository.findById(testOrder.getId()).orElseThrow();
            assertThat(updatedOrder.getStatus()).isEqualTo(OrderStatus.COMPLETED);
            assertThat(updatedOrder.getCompletedAt()).isNotNull();
            assertThat(updatedOrder.getPayment().getPaymentMethod()).isEqualTo(PAYPAL);

            // Verify tickets were marked as sold
            List<Long> ticketIds = orderTickets.stream().map(Ticket::getId).toList();
            List<Ticket> updatedTickets = ticketsRepository.findAllById(ticketIds);
            assertThat(updatedTickets).allSatisfy(ticket -> 
                assertThat(ticket.getStatus()).isEqualTo(SOLD)
            );

            // Verify raffle statistics were updated
            Raffle updatedRaffle = rafflesRepository.findById(testRaffle.getId()).orElseThrow();
            RaffleStatistics stats = updatedRaffle.getStatistics();
            assertThat(stats.getPendingOrders()).isEqualTo(0L); // decreased by 1
            assertThat(stats.getCompletedOrders()).isEqualTo(1L); // increased by 1
            assertThat(stats.getSoldTickets()).isEqualTo(2L); // increased by 2
            assertThat(stats.getRevenue()).usingComparator(BigDecimal::compareTo).isEqualTo(BigDecimal.valueOf(40.00)); // 2 tickets * 20.00
            assertThat(stats.getAverageOrderValue()).usingComparator(BigDecimal::compareTo).isEqualTo(BigDecimal.valueOf(40.00)); // 40.00/1 order
            assertThat(stats.getFirstSaleDate()).isNotNull();
            assertThat(stats.getLastSaleDate()).isNotNull();

            // Verify raffle remains active (not all tickets sold)
            assertThat(updatedRaffle.getStatus()).isEqualTo(ACTIVE);
        }

        @Test
        @DisplayName("Should complete raffle when all tickets are sold")
        void shouldCompleteRaffleWhenAllTicketsAreSold() throws Exception {
            // Arrange - Complete 4 tickets first, then complete the last ticket
            List<Ticket> firstOrderTickets = new ArrayList<>(testRaffle.getTickets().subList(0, 4));
            firstOrderTickets.forEach(ticket -> {
                ticket.setStatus(SOLD);
            });
            ticketsRepository.saveAll(firstOrderTickets);

            // Create order with the last ticket
            List<Ticket> lastTicket = new ArrayList<>(testRaffle.getTickets().subList(4, 5));
            Order testOrder = createTestOrder(PENDING, lastTicket, BigDecimal.valueOf(20.00));
            
            OrderComplete completeRequest = OrderComplete.builder()
                    .paymentMethod(CASH)
                    .build();

            String endpoint = baseEndpoint + "/" + testOrder.getId() + "/complete";

            // Act
            ResultActions result = mockMvc.perform(put(endpoint)
                    .with(user(authData.user().getEmail()))
                    .contentType(APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(completeRequest)));

            // Assert
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.status").value("COMPLETED"));

            // Verify raffle was completed
            Raffle updatedRaffle = rafflesRepository.findById(testRaffle.getId()).orElseThrow();
            assertThat(updatedRaffle.getStatus()).isEqualTo(COMPLETED);
            assertThat(updatedRaffle.getCompletedAt()).isNotNull();
            assertThat(updatedRaffle.getCompletionReason()).isEqualTo(ALL_TICKETS_SOLD);

            // Verify all tickets are sold
            List<Ticket> allTickets = ticketsRepository.findAllByRaffle(testRaffle);
            assertThat(allTickets).allSatisfy(ticket -> 
                assertThat(ticket.getStatus()).isEqualTo(SOLD)
            );
        }

        @Test
        @DisplayName("Should handle subsequent orders and update statistics correctly")
        void shouldHandleSubsequentOrdersAndUpdateStatisticsCorrectly() throws Exception {
            // Arrange - Complete first order to set baseline statistics
            List<Ticket> firstOrderTickets = new ArrayList<>(testRaffle.getTickets().subList(0, 2));
            Order firstOrder = createTestOrder(PENDING, firstOrderTickets, BigDecimal.valueOf(40.00));
            
            // Complete first order
            OrderComplete firstComplete = OrderComplete.builder().paymentMethod(VISA).build();
            mockMvc.perform(put(baseEndpoint + "/" + firstOrder.getId() + "/complete")
                    .with(user(authData.user().getEmail()))
                    .contentType(APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(firstComplete)));

            // Create second order
            List<Ticket> secondOrderTickets = new ArrayList<>(testRaffle.getTickets().subList(2, 4));
            Order secondOrder = createTestOrder(PENDING, secondOrderTickets, BigDecimal.valueOf(40.00));
            
            OrderComplete secondComplete = OrderComplete.builder()
                    .paymentMethod(MASTERCARD)
                    .build();

            String endpoint = baseEndpoint + "/" + secondOrder.getId() + "/complete";

            // Act - Complete second order
            ResultActions result = mockMvc.perform(put(endpoint)
                    .with(user(authData.user().getEmail()))
                    .contentType(APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(secondComplete)));

            // Assert
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));

            // Verify statistics after second completion
            Raffle updatedRaffle = rafflesRepository.findById(testRaffle.getId()).orElseThrow();
            RaffleStatistics stats = updatedRaffle.getStatistics();
            assertThat(stats.getCompletedOrders()).isEqualTo(2L); // 2 completed orders
            assertThat(stats.getSoldTickets()).isEqualTo(4L); // 4 tickets sold total
            assertThat(stats.getRevenue()).usingComparator(BigDecimal::compareTo).isEqualTo(BigDecimal.valueOf(80.00)); // 4 tickets * 20.00
            assertThat(stats.getAverageOrderValue()).usingComparator(BigDecimal::compareTo).isEqualTo(BigDecimal.valueOf(40.00)); 
            assertThat(stats.getLastSaleDate()).isNotNull();
        }
    }

    @Nested
    @DisplayName("PUT /v1/associations/{associationId}/orders/{orderId}/complete - Validation Failures")
    class ValidationFailureTests {

        @Test
        @DisplayName("Should fail when trying to complete already completed order")
        void shouldFailWhenTryingToCompleteAlreadyCompletedOrder() throws Exception {
            // Arrange
            List<Ticket> orderTickets = new ArrayList<>(testRaffle.getTickets().subList(0, 1));
            Order testOrder = createTestOrder(OrderStatus.COMPLETED, orderTickets, BigDecimal.valueOf(20.00));
            
            OrderComplete completeRequest = OrderComplete.builder()
                    .paymentMethod(PAYPAL)
                    .build();

            String endpoint = baseEndpoint + "/" + testOrder.getId() + "/complete";

            // Act
            ResultActions result = mockMvc.perform(put(endpoint)
                    .with(user(authData.user().getEmail()))
                    .contentType(APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(completeRequest)));

            // Assert
            result.andExpect(status().isBadRequest())
                    .andExpect(content().contentType("application/json"))
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("Unsupported status transition from COMPLETED to COMPLETED"));

            // Verify order was not changed
            Order unchangedOrder = ordersRepository.findById(testOrder.getId()).orElseThrow();
            assertThat(unchangedOrder.getStatus()).isEqualTo(OrderStatus.COMPLETED);
        }

        @Test
        @DisplayName("Should fail when trying to complete cancelled order")
        void shouldFailWhenTryingToCompleteCancelledOrder() throws Exception {
            // Arrange
            List<Ticket> orderTickets = new ArrayList<>(testRaffle.getTickets().subList(0, 1));
            Order testOrder = createTestOrder(CANCELLED, orderTickets, BigDecimal.valueOf(20.00));
            
            OrderComplete completeRequest = OrderComplete.builder()
                    .paymentMethod(PAYPAL)
                    .build();

            String endpoint = baseEndpoint + "/" + testOrder.getId() + "/complete";

            // Act
            ResultActions result = mockMvc.perform(put(endpoint)
                    .with(user(authData.user().getEmail()))
                    .contentType(APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(completeRequest)));

            // Assert
            result.andExpect(status().isBadRequest())
                    .andExpect(content().contentType("application/json"))
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("Unsupported status transition from CANCELLED to COMPLETED"));
        }

        @Test
        @DisplayName("Should fail when trying to complete unpaid order")
        void shouldFailWhenTryingToCompleteUnpaidOrder() throws Exception {
            // Arrange
            List<Ticket> orderTickets = new ArrayList<>(testRaffle.getTickets().subList(0, 1));
            Order testOrder = createTestOrder(UNPAID, orderTickets, BigDecimal.valueOf(20.00));
            
            OrderComplete completeRequest = OrderComplete.builder()
                    .paymentMethod(PAYPAL)
                    .build();

            String endpoint = baseEndpoint + "/" + testOrder.getId() + "/complete";

            // Act
            ResultActions result = mockMvc.perform(put(endpoint)
                    .with(user(authData.user().getEmail()))
                    .contentType(APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(completeRequest)));

            // Assert
            result.andExpect(status().isBadRequest())
                    .andExpect(content().contentType("application/json"))
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("Unsupported status transition from UNPAID to COMPLETED"));
        }

        @Test
        @DisplayName("Should fail when trying to complete refunded order")
        void shouldFailWhenTryingToCompleteRefundedOrder() throws Exception {
            // Arrange
            List<Ticket> orderTickets = testRaffle.getTickets().subList(0, 1);
            Order testOrder = createTestOrder(REFUNDED, orderTickets, BigDecimal.valueOf(20.00));
            
            OrderComplete completeRequest = OrderComplete.builder()
                    .paymentMethod(PAYPAL)
                    .build();

            String endpoint = baseEndpoint + "/" + testOrder.getId() + "/complete";

            // Act
            ResultActions result = mockMvc.perform(put(endpoint)
                    .with(user(authData.user().getEmail()))
                    .contentType(APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(completeRequest)));

            // Assert
            result.andExpect(status().isBadRequest())
                    .andExpect(content().contentType("application/json"))
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("Unsupported status transition from REFUNDED to COMPLETED"));
        }

        @Test
        @DisplayName("Should fail when order does not exist")
        void shouldFailWhenOrderDoesNotExist() throws Exception {
            // Arrange
            Long nonExistentOrderId = 99999L;
            OrderComplete completeRequest = OrderComplete.builder()
                    .paymentMethod(PAYPAL)
                    .build();

            String endpoint = baseEndpoint + "/" + nonExistentOrderId + "/complete";

            // Act
            ResultActions result = mockMvc.perform(put(endpoint)
                    .with(user(authData.user().getEmail()))
                    .contentType(APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(completeRequest)));

            // Assert
            result.andExpect(status().isNotFound())
                    .andExpect(content().contentType("application/json"))
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("Should handle invalid payment method by converting to UNKNOWN")
        void shouldHandleInvalidPaymentMethodByConvertingToUnknown() throws Exception {
            // Arrange
            List<Ticket> orderTickets = testRaffle.getTickets().subList(0, 1);
            Order testOrder = createTestOrder(PENDING, orderTickets, BigDecimal.valueOf(20.00));
            
            String endpoint = baseEndpoint + "/" + testOrder.getId() + "/complete";

            // Act - Send request with invalid payment method
            ResultActions result = mockMvc.perform(put(endpoint)
                    .with(user(authData.user().getEmail()))
                    .contentType(APPLICATION_JSON)
                    .content("{\"paymentMethod\": \"INVALID_METHOD\"}"));

            // Assert - Should succeed and convert invalid method to UNKNOWN
            result.andExpect(status().isOk())
                    .andExpect(content().contentType("application/json"))
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.status").value("COMPLETED"))
                    .andExpect(jsonPath("$.data.payment.paymentMethod").value("unknown"));

            // Verify order was completed with UNKNOWN payment method
            Order updatedOrder = ordersRepository.findById(testOrder.getId()).orElseThrow();
            assertThat(updatedOrder.getStatus()).isEqualTo(OrderStatus.COMPLETED);
            assertThat(updatedOrder.getPayment().getPaymentMethod()).isEqualTo(PaymentMethod.UNKNOWN);
        }

        @Test
        @DisplayName("Should fail when payment method is null")
        void shouldFailWhenPaymentMethodIsNull() throws Exception {
            // Arrange
            List<Ticket> orderTickets = testRaffle.getTickets().subList(0, 1);
            Order testOrder = createTestOrder(PENDING, orderTickets, BigDecimal.valueOf(20.00));
            
            String endpoint = baseEndpoint + "/" + testOrder.getId() + "/complete";

            // Act - Send request without payment method
            ResultActions result = mockMvc.perform(put(endpoint)
                    .with(user(authData.user().getEmail()))
                    .contentType(APPLICATION_JSON)
                    .content("{}"));

            // Assert - Should fail due to @NotNull validation
            result.andExpect(status().isBadRequest())
                    .andExpect(content().contentType("application/json"))
                    .andExpect(jsonPath("$.success").value(false));
        }
    }

    @Nested
    @DisplayName("PUT /v1/associations/{associationId}/orders/{orderId}/complete - Edge Cases")
    class EdgeCaseTests {

        @Test
        @DisplayName("Should handle completion with different payment methods correctly")
        void shouldHandleCompletionWithDifferentPaymentMethodsCorrectly() throws Exception {
            // Test different payment methods
            PaymentMethod[] paymentMethods = {VISA, MASTERCARD, AMERICAN_EXPRESS, PAYPAL, APPLE_PAY, CASH, BANK_TRANSFER};
            
            for (int i = 0; i < paymentMethods.length && i < testRaffle.getTickets().size(); i++) {
                // Arrange
                List<Ticket> orderTickets = List.of(testRaffle.getTickets().get(i));
                Order testOrder = createTestOrder(PENDING, orderTickets, BigDecimal.valueOf(20.00));
                
                OrderComplete completeRequest = OrderComplete.builder()
                        .paymentMethod(paymentMethods[i])
                        .build();

                String endpoint = baseEndpoint + "/" + testOrder.getId() + "/complete";

                // Act
                ResultActions result = mockMvc.perform(put(endpoint)
                        .with(user(authData.user().getEmail()))
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(completeRequest)));

                // Assert
                result.andExpect(status().isOk())
                        .andExpect(jsonPath("$.success").value(true))
                        .andExpect(jsonPath("$.data.payment.paymentMethod").value(paymentMethods[i].getValue()));

                // Verify payment method was updated
                Order updatedOrder = ordersRepository.findById(testOrder.getId()).orElseThrow();
                assertThat(updatedOrder.getPayment().getPaymentMethod()).isEqualTo(paymentMethods[i]);
            }
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
                    .total(BigDecimal.valueOf(20.00))
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

            OrderComplete completeRequest = OrderComplete.builder()
                    .paymentMethod(PAYPAL)
                    .build();

            String endpoint = baseEndpoint + "/" + testOrder.getId() + "/complete";

            // Act
            ResultActions result = mockMvc.perform(put(endpoint)
                    .with(user(authData.user().getEmail()))
                    .contentType(APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(completeRequest)));

            // Assert - Should fail with NotFoundException
            result.andExpect(status().isNotFound())
                    .andExpect(content().contentType("application/json"))
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("Some tickets were not found"));

            // Verify order was not changed
            Order unchangedOrder = ordersRepository.findById(testOrder.getId()).orElseThrow();
            assertThat(unchangedOrder.getStatus()).isEqualTo(PENDING);
            assertThat(unchangedOrder.getCompletedAt()).isNull();
        }
    }
} 