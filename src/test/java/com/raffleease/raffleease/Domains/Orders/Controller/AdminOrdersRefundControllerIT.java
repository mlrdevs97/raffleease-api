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
import static com.raffleease.raffleease.Domains.Orders.Model.OrderStatus.COMPLETED;
import static com.raffleease.raffleease.Domains.Orders.Model.OrderStatus.PENDING;
import static com.raffleease.raffleease.Domains.Payments.Model.PaymentMethod.CARD;
import static com.raffleease.raffleease.Domains.Raffles.Model.CompletionReason.ALL_TICKETS_SOLD;
import static com.raffleease.raffleease.Domains.Raffles.Model.RaffleStatus.*;
import static com.raffleease.raffleease.Domains.Tickets.Model.TicketStatus.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("Admin Orders Refund Controller Integration Tests")
class AdminOrdersRefundControllerIT extends AbstractIntegrationTest {

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
        // Create raffle statistics with some completed orders and revenue
        initialStatistics = TestDataBuilder.statistics()
                .availableTickets(2L)
                .soldTickets(3L)
                .revenue(BigDecimal.valueOf(75.00))
                .participants(2L)
                .totalOrders(3L)
                .pendingOrders(0L)
                .completedOrders(3L)
                .cancelledOrders(0L)
                .unpaidOrders(0L)
                .refundedOrders(0L)
                .averageOrderValue(BigDecimal.valueOf(25.00))
                .firstSaleDate(LocalDateTime.now().minusDays(5))
                .lastSaleDate(LocalDateTime.now().minusDays(1))
                .build();

        // Create test raffle with 5 tickets
        testRaffle = TestDataBuilder.raffle()
                .association(authData.association())
                .status(ACTIVE)
                .title("Test Raffle for Order Refund")
                .ticketPrice(BigDecimal.valueOf(25.00))
                .totalTickets(5L)
                .firstTicketNumber(1L)
                .endDate(LocalDateTime.now().plusDays(30)) // Future end date
                .statistics(initialStatistics)
                .build();
        testRaffle = rafflesRepository.save(testRaffle);

        // Create tickets for the raffle
        List<Ticket> tickets = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            TicketStatus status = i <= 3 ? SOLD : AVAILABLE; // First 3 tickets sold
            Ticket ticket = Ticket.builder()
                    .ticketNumber(String.valueOf(i))
                    .status(status)
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
                .orderReference("ORD-REFUND-" + System.currentTimeMillis())
                .comment("Test refund order")
                .orderItems(new ArrayList<>())
                .build();
        
        if (status == COMPLETED) {
            order.setCompletedAt(LocalDateTime.now().minusDays(1));
        }
        
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

        // Set tickets as SOLD and assign to customer for completed orders
        if (status == COMPLETED) {
            Customer finalCustomer = customer;
            orderTickets.forEach(ticket -> {
                ticket.setStatus(SOLD);
                ticket.setCustomer(finalCustomer);
            });
            ticketsRepository.saveAll(orderTickets);
        }

        return ordersRepository.save(order);
    }

    @Nested
    @DisplayName("PUT /v1/associations/{associationId}/orders/{orderId}/refund - Authorization Tests")
    class AuthorizationTests {

        @Test
        @DisplayName("Should return 403 when COLLABORATOR tries to refund order")
        void shouldReturn403WhenCollaboratorTriesToRefundOrder() throws Exception {
            // Arrange
            AuthTestData collaboratorData = authTestUtils.createAuthenticatedUserInSameAssociation(
                    authData.association(), AssociationRole.COLLABORATOR);
            
            List<Ticket> orderTickets = new ArrayList<>(testRaffle.getTickets().subList(0, 1));
            Order testOrder = createTestOrder(COMPLETED, orderTickets, BigDecimal.valueOf(25.00));
            
            String endpoint = baseEndpoint + "/" + testOrder.getId() + "/refund";

            // Act
            ResultActions result = mockMvc.perform(put(endpoint)
                    .with(user(collaboratorData.user().getEmail())));

            // Assert
            result.andExpect(status().isForbidden())
                    .andExpect(content().contentType("application/json"))
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("Only administrators and members can refund orders"));

            // Verify order was not changed
            Order unchangedOrder = ordersRepository.findById(testOrder.getId()).orElseThrow();
            assertThat(unchangedOrder.getStatus()).isEqualTo(COMPLETED);
            assertThat(unchangedOrder.getRefundedAt()).isNull();
        }

        @Test
        @DisplayName("Should successfully refund order for ADMIN role")
        void shouldSuccessfullyRefundOrderForAdmin() throws Exception {
            // Arrange
            AuthTestData adminData = authTestUtils.createAuthenticatedUserInSameAssociation(
                    authData.association(), AssociationRole.ADMIN);
            
            List<Ticket> orderTickets = new ArrayList<>(testRaffle.getTickets().subList(0, 1));
            Order testOrder = createTestOrder(COMPLETED, orderTickets, BigDecimal.valueOf(25.00));
            
            String endpoint = baseEndpoint + "/" + testOrder.getId() + "/refund";

            // Act
            ResultActions result = mockMvc.perform(put(endpoint)
                    .with(user(adminData.user().getEmail())));

            // Assert
            result.andExpect(status().isOk())
                    .andExpect(content().contentType("application/json"))
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Order refunded successfully"))
                    .andExpect(jsonPath("$.data.status").value("REFUNDED"));

            // Verify order was refunded
            Order refundedOrder = ordersRepository.findById(testOrder.getId()).orElseThrow();
            assertThat(refundedOrder.getStatus()).isEqualTo(REFUNDED);
            assertThat(refundedOrder.getRefundedAt()).isNotNull();
        }

        @Test
        @DisplayName("Should successfully refund order for MEMBER role")
        void shouldSuccessfullyRefundOrderForMember() throws Exception {
            // Arrange
            AuthTestData memberData = authTestUtils.createAuthenticatedUserInSameAssociation(
                    authData.association(), AssociationRole.MEMBER);
            
            List<Ticket> orderTickets = new ArrayList<>(testRaffle.getTickets().subList(0, 1));
            Order testOrder = createTestOrder(COMPLETED, orderTickets, BigDecimal.valueOf(25.00));
            
            String endpoint = baseEndpoint + "/" + testOrder.getId() + "/refund";

            // Act
            ResultActions result = mockMvc.perform(put(endpoint)
                    .with(user(memberData.user().getEmail())));

            // Assert
            result.andExpect(status().isOk())
                    .andExpect(content().contentType("application/json"))
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Order refunded successfully"))
                    .andExpect(jsonPath("$.data.status").value("REFUNDED"));

            // Verify order was refunded
            Order refundedOrder = ordersRepository.findById(testOrder.getId()).orElseThrow();
            assertThat(refundedOrder.getStatus()).isEqualTo(REFUNDED);
            assertThat(refundedOrder.getRefundedAt()).isNotNull();
        }

        @Test
        @DisplayName("Should return 401 when user is not authenticated")
        void shouldReturn401WhenNotAuthenticated() throws Exception {
            // Arrange
            List<Ticket> orderTickets = new ArrayList<>(testRaffle.getTickets().subList(0, 1));
            Order testOrder = createTestOrder(COMPLETED, orderTickets, BigDecimal.valueOf(25.00));
            
            String endpoint = baseEndpoint + "/" + testOrder.getId() + "/refund";

            // Act
            ResultActions result = mockMvc.perform(put(endpoint));

            // Assert
            result.andExpect(status().isUnauthorized());

            // Verify order was not changed
            Order unchangedOrder = ordersRepository.findById(testOrder.getId()).orElseThrow();
            assertThat(unchangedOrder.getStatus()).isEqualTo(COMPLETED);
            assertThat(unchangedOrder.getRefundedAt()).isNull();
        }
    }

    @Nested
    @DisplayName("PUT /v1/associations/{associationId}/orders/{orderId}/refund - Successful Refund")
    class SuccessfulRefundTests {

        @Test
        @DisplayName("Should successfully refund a completed order")
        void shouldSuccessfullyRefundCompletedOrder() throws Exception {
            // Arrange
            List<Ticket> orderTickets = new ArrayList<>(testRaffle.getTickets().subList(0, 2)); // 2 tickets
            Order testOrder = createTestOrder(COMPLETED, orderTickets, BigDecimal.valueOf(50.00));
            
            String endpoint = baseEndpoint + "/" + testOrder.getId() + "/refund";

            // Act
            ResultActions result = mockMvc.perform(put(endpoint)
                    .with(user(authData.user().getEmail())));

            // Assert
            result.andExpect(status().isOk())
                    .andExpect(content().contentType("application/json"))
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Order refunded successfully"))
                    .andExpect(jsonPath("$.data.id").value(testOrder.getId()))
                    .andExpect(jsonPath("$.data.status").value("REFUNDED"));

            // Verify order was updated
            Order updatedOrder = ordersRepository.findById(testOrder.getId()).orElseThrow();
            assertThat(updatedOrder.getStatus()).isEqualTo(REFUNDED);
            assertThat(updatedOrder.getRefundedAt()).isNotNull();

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
            assertThat(stats.getRefundedOrders()).isEqualTo(1L); // increased by 1
            assertThat(stats.getCompletedOrders()).isEqualTo(2L); // decreased by 1 (from 3 to 2)
            assertThat(stats.getSoldTickets()).isEqualTo(1L); // decreased by 2 (from 3 to 1)
            assertThat(stats.getAvailableTickets()).isEqualTo(4L); // increased by 2 (from 2 to 4)
            assertThat(stats.getRevenue()).usingComparator(BigDecimal::compareTo).isEqualTo(BigDecimal.valueOf(25.00)); // decreased by 50.00 (from 75.00 to 25.00)
            assertThat(stats.getAverageOrderValue()).usingComparator(BigDecimal::compareTo).isEqualTo(BigDecimal.valueOf(12.50)); // recalculated: 25.00 / 2 = 12.50

            // Verify raffle remains active (not completed by all tickets sold)
            assertThat(updatedRaffle.getStatus()).isEqualTo(ACTIVE);
        }

        @Test
        @DisplayName("Should handle refund with different ticket quantities")
        void shouldHandleRefundWithDifferentTicketQuantities() throws Exception {
            // Test refunding orders with different numbers of tickets
            int[] ticketCounts = {1, 3};
            
            for (int ticketCount : ticketCounts) {
                // Reset raffle state
                setupTestRaffle();
                
                // Arrange
                List<Ticket> orderTickets = new ArrayList<>(testRaffle.getTickets().subList(0, ticketCount));
                Order testOrder = createTestOrder(COMPLETED, orderTickets, BigDecimal.valueOf(25.00 * ticketCount));
                
                String endpoint = baseEndpoint + "/" + testOrder.getId() + "/refund";

                // Act
                ResultActions result = mockMvc.perform(put(endpoint)
                        .with(user(authData.user().getEmail())));

                // Assert
                result.andExpect(status().isOk())
                        .andExpect(jsonPath("$.success").value(true))
                        .andExpect(jsonPath("$.data.status").value("REFUNDED"));

                // Verify tickets were released
                List<Long> ticketIds = orderTickets.stream().map(Ticket::getId).toList();
                List<Ticket> updatedTickets = ticketsRepository.findAllById(ticketIds);
                assertThat(updatedTickets).allSatisfy(ticket -> 
                    assertThat(ticket.getStatus()).isEqualTo(AVAILABLE)
                );

                // Verify statistics updated correctly
                Raffle updatedRaffle = rafflesRepository.findById(testRaffle.getId()).orElseThrow();
                RaffleStatistics stats = updatedRaffle.getStatistics();
                assertThat(stats.getRefundedOrders()).isEqualTo(1L);
                assertThat(stats.getSoldTickets()).isEqualTo(3L - ticketCount); // original 3 minus refunded tickets
                assertThat(stats.getAvailableTickets()).isEqualTo(2L + ticketCount); // original 2 plus refunded tickets
            }
        }

        @Test
        @DisplayName("Should handle multiple order refunds and update statistics correctly")
        void shouldHandleMultipleOrderRefundsAndUpdateStatisticsCorrectly() throws Exception {
            // Arrange - Create and refund first order
            List<Ticket> firstOrderTickets = new ArrayList<>(testRaffle.getTickets().subList(0, 1));
            Order firstOrder = createTestOrder(COMPLETED, firstOrderTickets, BigDecimal.valueOf(25.00));
            
            // Refund first order
            mockMvc.perform(put(baseEndpoint + "/" + firstOrder.getId() + "/refund")
                    .with(user(authData.user().getEmail())));

            // Create second order
            List<Ticket> secondOrderTickets = new ArrayList<>(testRaffle.getTickets().subList(1, 3));
            Order secondOrder = createTestOrder(COMPLETED, secondOrderTickets, BigDecimal.valueOf(50.00));
            
            String endpoint = baseEndpoint + "/" + secondOrder.getId() + "/refund";

            // Act - Refund second order
            ResultActions result = mockMvc.perform(put(endpoint)
                    .with(user(authData.user().getEmail())));

            // Assert
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));

            // Verify statistics after second refund
            Raffle updatedRaffle = rafflesRepository.findById(testRaffle.getId()).orElseThrow();
            RaffleStatistics stats = updatedRaffle.getStatistics();
            assertThat(stats.getRefundedOrders()).isEqualTo(2L); // 2 refunded orders
            assertThat(stats.getCompletedOrders()).isEqualTo(1L); // 3 - 2 = 1 completed order left
            assertThat(stats.getSoldTickets()).isEqualTo(0L); // all 3 tickets refunded
            assertThat(stats.getAvailableTickets()).isEqualTo(5L); // all tickets available again
            assertThat(stats.getRevenue()).usingComparator(BigDecimal::compareTo).isEqualTo(BigDecimal.ZERO); // all revenue refunded
            assertThat(stats.getAverageOrderValue()).usingComparator(BigDecimal::compareTo).isEqualTo(BigDecimal.ZERO); // no completed orders left
        }

        @Test
        @DisplayName("Should handle refund when all completed orders result in zero average order value")
        void shouldHandleRefundWhenAllCompletedOrdersResultInZeroAverageOrderValue() throws Exception {
            // Arrange - Create a scenario where we have only 1 completed order
            // Reset statistics to have only 1 completed order
            initialStatistics.setCompletedOrders(1L);
            initialStatistics.setRevenue(BigDecimal.valueOf(25.00));
            initialStatistics.setAverageOrderValue(BigDecimal.valueOf(25.00));
            testRaffle.setStatistics(initialStatistics);
            rafflesRepository.save(testRaffle);
            
            List<Ticket> orderTickets = new ArrayList<>(testRaffle.getTickets().subList(0, 1));
            Order testOrder = createTestOrder(COMPLETED, orderTickets, BigDecimal.valueOf(25.00));
            
            String endpoint = baseEndpoint + "/" + testOrder.getId() + "/refund";

            // Act
            ResultActions result = mockMvc.perform(put(endpoint)
                    .with(user(authData.user().getEmail())));

            // Assert
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));

            // Verify statistics - average order value should be zero when no completed orders remain
            Raffle updatedRaffle = rafflesRepository.findById(testRaffle.getId()).orElseThrow();
            RaffleStatistics stats = updatedRaffle.getStatistics();
            assertThat(stats.getCompletedOrders()).isEqualTo(0L);
            assertThat(stats.getRefundedOrders()).isEqualTo(1L);
            assertThat(stats.getRevenue()).usingComparator(BigDecimal::compareTo).isEqualTo(BigDecimal.ZERO);
            assertThat(stats.getAverageOrderValue()).usingComparator(BigDecimal::compareTo).isEqualTo(BigDecimal.ZERO);
        }
    }

    @Nested
    @DisplayName("PUT /v1/associations/{associationId}/orders/{orderId}/refund - Raffle Reactivation")
    class RaffleReactivationTests {

        @Test
        @DisplayName("Should reactivate raffle when it was completed due to all tickets sold")
        void shouldReactivateRaffleWhenItWasCompletedDueToAllTicketsSold() throws Exception {
            // Arrange - Set up raffle as COMPLETED with ALL_TICKETS_SOLD reason
            testRaffle.setStatus(RaffleStatus.COMPLETED);
            testRaffle.setCompletionReason(ALL_TICKETS_SOLD);
            testRaffle.setCompletedAt(LocalDateTime.now().minusHours(1));
            testRaffle.setEndDate(LocalDateTime.now().plusDays(30)); // Future end date
            rafflesRepository.save(testRaffle);

            // Create completed order with sold tickets
            List<Ticket> orderTickets = new ArrayList<>(testRaffle.getTickets().subList(0, 2));
            Order testOrder = createTestOrder(COMPLETED, orderTickets, BigDecimal.valueOf(50.00));
            
            String endpoint = baseEndpoint + "/" + testOrder.getId() + "/refund";

            // Act
            ResultActions result = mockMvc.perform(put(endpoint)
                    .with(user(authData.user().getEmail())));

            // Assert
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));

            // Verify raffle was reactivated
            Raffle updatedRaffle = rafflesRepository.findById(testRaffle.getId()).orElseThrow();
            assertThat(updatedRaffle.getStatus()).isEqualTo(ACTIVE);
            assertThat(updatedRaffle.getCompletionReason()).isNull();
            assertThat(updatedRaffle.getCompletedAt()).isNull();
        }

        @Test
        @DisplayName("Should not reactivate raffle when it was completed due to end date reached")
        void shouldNotReactivateRaffleWhenItWasCompletedDueToEndDateReached() throws Exception {
            // Arrange - Set up raffle as COMPLETED with END_DATE_REACHED reason
            testRaffle.setStatus(RaffleStatus.COMPLETED);
            testRaffle.setCompletionReason(CompletionReason.END_DATE_REACHED);
            testRaffle.setCompletedAt(LocalDateTime.now().minusHours(1));
            testRaffle.setEndDate(LocalDateTime.now().minusDays(1)); // Past end date
            rafflesRepository.save(testRaffle);

            // Create completed order
            List<Ticket> orderTickets = new ArrayList<>(testRaffle.getTickets().subList(0, 1));
            Order testOrder = createTestOrder(COMPLETED, orderTickets, BigDecimal.valueOf(25.00));
            
            String endpoint = baseEndpoint + "/" + testOrder.getId() + "/refund";

            // Act
            ResultActions result = mockMvc.perform(put(endpoint)
                    .with(user(authData.user().getEmail())));

            // Assert
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));

            // Verify raffle was NOT reactivated (remains completed)
            Raffle updatedRaffle = rafflesRepository.findById(testRaffle.getId()).orElseThrow();
            assertThat(updatedRaffle.getStatus()).isEqualTo(RaffleStatus.COMPLETED);
            assertThat(updatedRaffle.getCompletionReason()).isEqualTo(CompletionReason.END_DATE_REACHED);
            assertThat(updatedRaffle.getCompletedAt()).isNotNull();
        }

        @Test
        @DisplayName("Should not reactivate raffle when it was manually completed")
        void shouldNotReactivateRaffleWhenItWasManuallyCompleted() throws Exception {
            // Arrange - Set up raffle as COMPLETED with MANUALLY_COMPLETED reason
            testRaffle.setStatus(RaffleStatus.COMPLETED);
            testRaffle.setCompletionReason(CompletionReason.MANUALLY_COMPLETED);
            testRaffle.setCompletedAt(LocalDateTime.now().minusHours(1));
            rafflesRepository.save(testRaffle);

            // Create completed order
            List<Ticket> orderTickets = new ArrayList<>(testRaffle.getTickets().subList(0, 1));
            Order testOrder = createTestOrder(COMPLETED, orderTickets, BigDecimal.valueOf(25.00));
            
            String endpoint = baseEndpoint + "/" + testOrder.getId() + "/refund";

            // Act
            ResultActions result = mockMvc.perform(put(endpoint)
                    .with(user(authData.user().getEmail())));

            // Assert
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));

            // Verify raffle was NOT reactivated (remains completed)
            Raffle updatedRaffle = rafflesRepository.findById(testRaffle.getId()).orElseThrow();
            assertThat(updatedRaffle.getStatus()).isEqualTo(RaffleStatus.COMPLETED);
            assertThat(updatedRaffle.getCompletionReason()).isEqualTo(CompletionReason.MANUALLY_COMPLETED);
            assertThat(updatedRaffle.getCompletedAt()).isNotNull();
        }
    }

    @Nested
    @DisplayName("PUT /v1/associations/{associationId}/orders/{orderId}/refund - Validation Failures")
    class ValidationFailureTests {

        @Test
        @DisplayName("Should fail when trying to refund pending order")
        void shouldFailWhenTryingToRefundPendingOrder() throws Exception {
            // Arrange
            List<Ticket> orderTickets = new ArrayList<>(testRaffle.getTickets().subList(0, 1));
            Order testOrder = createTestOrder(PENDING, orderTickets, BigDecimal.valueOf(25.00));
            
            String endpoint = baseEndpoint + "/" + testOrder.getId() + "/refund";

            // Act
            ResultActions result = mockMvc.perform(put(endpoint)
                    .with(user(authData.user().getEmail())));

            // Assert
            result.andExpect(status().isBadRequest())
                    .andExpect(content().contentType("application/json"))
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("Unsupported status transition from PENDING to REFUNDED"));

            // Verify order was not changed
            Order unchangedOrder = ordersRepository.findById(testOrder.getId()).orElseThrow();
            assertThat(unchangedOrder.getStatus()).isEqualTo(PENDING);
            assertThat(unchangedOrder.getRefundedAt()).isNull();
        }

        @Test
        @DisplayName("Should fail when trying to refund cancelled order")
        void shouldFailWhenTryingToRefundCancelledOrder() throws Exception {
            // Arrange
            List<Ticket> orderTickets = new ArrayList<>(testRaffle.getTickets().subList(0, 1));
            Order testOrder = createTestOrder(CANCELLED, orderTickets, BigDecimal.valueOf(25.00));
            
            String endpoint = baseEndpoint + "/" + testOrder.getId() + "/refund";

            // Act
            ResultActions result = mockMvc.perform(put(endpoint)
                    .with(user(authData.user().getEmail())));

            // Assert
            result.andExpect(status().isBadRequest())
                    .andExpect(content().contentType("application/json"))
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("Unsupported status transition from CANCELLED to REFUNDED"));
        }

        @Test
        @DisplayName("Should fail when trying to refund unpaid order")
        void shouldFailWhenTryingToRefundUnpaidOrder() throws Exception {
            // Arrange
            List<Ticket> orderTickets = new ArrayList<>(testRaffle.getTickets().subList(0, 1));
            Order testOrder = createTestOrder(UNPAID, orderTickets, BigDecimal.valueOf(25.00));
            
            String endpoint = baseEndpoint + "/" + testOrder.getId() + "/refund";

            // Act
            ResultActions result = mockMvc.perform(put(endpoint)
                    .with(user(authData.user().getEmail())));

            // Assert
            result.andExpect(status().isBadRequest())
                    .andExpect(content().contentType("application/json"))
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("Unsupported status transition from UNPAID to REFUNDED"));
        }

        @Test
        @DisplayName("Should fail when trying to refund already refunded order")
        void shouldFailWhenTryingToRefundAlreadyRefundedOrder() throws Exception {
            // Arrange
            List<Ticket> orderTickets = new ArrayList<>(testRaffle.getTickets().subList(0, 1));
            Order testOrder = createTestOrder(REFUNDED, orderTickets, BigDecimal.valueOf(25.00));
            
            String endpoint = baseEndpoint + "/" + testOrder.getId() + "/refund";

            // Act
            ResultActions result = mockMvc.perform(put(endpoint)
                    .with(user(authData.user().getEmail())));

            // Assert
            result.andExpect(status().isBadRequest())
                    .andExpect(content().contentType("application/json"))
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("Unsupported status transition from REFUNDED to REFUNDED"));
        }

        @Test
        @DisplayName("Should fail when order does not exist")
        void shouldFailWhenOrderDoesNotExist() throws Exception {
            // Arrange
            Long nonExistentOrderId = 99999L;
            String endpoint = baseEndpoint + "/" + nonExistentOrderId + "/refund";

            // Act
            ResultActions result = mockMvc.perform(put(endpoint)
                    .with(user(authData.user().getEmail())));

            // Assert
            result.andExpect(status().isNotFound())
                    .andExpect(content().contentType("application/json"))
                    .andExpect(jsonPath("$.success").value(false));
        }
    }

    @Nested
    @DisplayName("PUT /v1/associations/{associationId}/orders/{orderId}/refund - Edge Cases")
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
                    .status(COMPLETED)
                    .orderReference("ORD-EMPTY-" + System.currentTimeMillis())
                    .completedAt(LocalDateTime.now().minusDays(1))
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

            String endpoint = baseEndpoint + "/" + testOrder.getId() + "/refund";

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
            assertThat(unchangedOrder.getStatus()).isEqualTo(COMPLETED);
            assertThat(unchangedOrder.getRefundedAt()).isNull();
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
                    .status(COMPLETED)
                    .orderReference("ORD-INVALID-" + System.currentTimeMillis())
                    .completedAt(LocalDateTime.now().minusDays(1))
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

            String endpoint = baseEndpoint + "/" + testOrder.getId() + "/refund";

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
            assertThat(unchangedOrder.getStatus()).isEqualTo(COMPLETED);
            assertThat(unchangedOrder.getRefundedAt()).isNull();
        }
    }
} 