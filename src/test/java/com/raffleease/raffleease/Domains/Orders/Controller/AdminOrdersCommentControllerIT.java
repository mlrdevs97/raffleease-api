package com.raffleease.raffleease.Domains.Orders.Controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.raffleease.raffleease.Base.AbstractIntegrationTest;
import com.raffleease.raffleease.Domains.Customers.Model.Customer;
import com.raffleease.raffleease.Domains.Customers.Repository.CustomersRepository;
import com.raffleease.raffleease.Domains.Orders.DTOs.CommentRequest;
import com.raffleease.raffleease.Domains.Orders.Model.Order;
import com.raffleease.raffleease.Domains.Orders.Model.OrderItem;
import com.raffleease.raffleease.Domains.Orders.Model.OrderStatus;
import com.raffleease.raffleease.Domains.Orders.Repository.OrdersRepository;
import com.raffleease.raffleease.Domains.Payments.Model.Payment;
import com.raffleease.raffleease.Domains.Payments.Repository.PaymentsRepository;
import com.raffleease.raffleease.Domains.Raffles.Model.Raffle;
import com.raffleease.raffleease.Domains.Raffles.Model.RaffleStatistics;
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
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static com.raffleease.raffleease.Domains.Orders.Model.OrderStatus.*;
import static com.raffleease.raffleease.Domains.Payments.Model.PaymentMethod.CARD;
import static com.raffleease.raffleease.Domains.Raffles.Model.RaffleStatus.ACTIVE;
import static com.raffleease.raffleease.Domains.Tickets.Model.TicketStatus.AVAILABLE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("Admin Orders Comment Controller Integration Tests")
class AdminOrdersCommentControllerIT extends AbstractIntegrationTest {

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

    @BeforeEach
    void setUp() {
        authData = authTestUtils.createAuthenticatedUser();
        baseEndpoint = "/v1/associations/" + authData.association().getId() + "/orders";
        setupTestRaffle();
    }

    private void setupTestRaffle() {
        // Create raffle statistics
        RaffleStatistics initialStatistics = TestDataBuilder.statistics()
                .availableTickets(5L)
                .soldTickets(0L)
                .revenue(BigDecimal.ZERO)
                .participants(0L)
                .totalOrders(0L)
                .pendingOrders(0L)
                .completedOrders(0L)
                .cancelledOrders(0L)
                .unpaidOrders(0L)
                .refundedOrders(0L)
                .averageOrderValue(BigDecimal.ZERO)
                .build();

        // Create test raffle
        testRaffle = TestDataBuilder.raffle()
                .association(authData.association())
                .status(ACTIVE)
                .title("Test Raffle for Comments")
                .ticketPrice(BigDecimal.valueOf(25.00))
                .totalTickets(5L)
                .firstTicketNumber(1L)
                .endDate(LocalDateTime.now().plusDays(30))
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

    private Order createTestOrder(OrderStatus status, String initialComment) {
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
                .orderReference("ORD-COMMENT-" + System.currentTimeMillis())
                .comment(initialComment)
                .orderItems(new ArrayList<>())
                .build();
        order = ordersRepository.save(order);

        // Create payment
        Payment payment = Payment.builder()
                .order(order)
                .total(BigDecimal.valueOf(50.00))
                .paymentMethod(CARD)
                .build();
        payment = paymentsRepository.save(payment);
        order.setPayment(payment);

        // Create order items
        List<Ticket> orderTickets = testRaffle.getTickets().subList(0, 2);
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

        return ordersRepository.save(order);
    }

    @Nested
    @DisplayName("POST /v1/associations/{associationId}/orders/{orderId}/comment - Add Comment")
    class AddCommentTests {

        @Test
        @DisplayName("Should successfully add comment to order without existing comment")
        void shouldSuccessfullyAddCommentToOrderWithoutExistingComment() throws Exception {
            // Arrange
            Order testOrder = createTestOrder(PENDING, null);
            CommentRequest commentRequest = CommentRequest.builder()
                    .comment("This is a new comment")
                    .build();
            
            String endpoint = baseEndpoint + "/" + testOrder.getId() + "/comment";

            // Act
            ResultActions result = mockMvc.perform(post(endpoint)
                    .with(user(authData.user().getEmail()))
                    .contentType(APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(commentRequest)));

            // Assert
            result.andExpect(status().isOk())
                    .andExpect(content().contentType("application/json"))
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Order comment added successfully"))
                    .andExpect(jsonPath("$.data.id").value(testOrder.getId()))
                    .andExpect(jsonPath("$.data.comment").value("This is a new comment"));

            // Verify comment was added to database
            Order updatedOrder = ordersRepository.findById(testOrder.getId()).orElseThrow();
            assertThat(updatedOrder.getComment()).isEqualTo("This is a new comment");
        }

        @Test
        @DisplayName("Should successfully add comment to order with existing comment (overwrites)")
        void shouldSuccessfullyAddCommentToOrderWithExistingComment() throws Exception {
            // Arrange
            Order testOrder = createTestOrder(PENDING, "Original comment");
            CommentRequest commentRequest = CommentRequest.builder()
                    .comment("Updated comment")
                    .build();
            
            String endpoint = baseEndpoint + "/" + testOrder.getId() + "/comment";

            // Act
            ResultActions result = mockMvc.perform(post(endpoint)
                    .with(user(authData.user().getEmail()))
                    .contentType(APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(commentRequest)));

            // Assert
            result.andExpect(status().isOk())
                    .andExpect(content().contentType("application/json"))
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Order comment added successfully"))
                    .andExpect(jsonPath("$.data.comment").value("Updated comment"));

            // Verify comment was updated in database
            Order updatedOrder = ordersRepository.findById(testOrder.getId()).orElseThrow();
            assertThat(updatedOrder.getComment()).isEqualTo("Updated comment");
        }

        @Test
        @DisplayName("Should successfully add comment to orders with different statuses")
        void shouldSuccessfullyAddCommentToOrdersWithDifferentStatuses() throws Exception {
            // Test adding comments to orders with different statuses
            OrderStatus[] statuses = {PENDING, COMPLETED, CANCELLED, UNPAID, REFUNDED};
            
            for (OrderStatus status : statuses) {
                // Arrange
                Order testOrder = createTestOrder(status, null);
                CommentRequest commentRequest = CommentRequest.builder()
                        .comment("Comment for " + status + " order")
                        .build();
                
                String endpoint = baseEndpoint + "/" + testOrder.getId() + "/comment";

                // Act
                ResultActions result = mockMvc.perform(post(endpoint)
                        .with(user(authData.user().getEmail()))
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(commentRequest)));

                // Assert
                result.andExpect(status().isOk())
                        .andExpect(jsonPath("$.success").value(true))
                        .andExpect(jsonPath("$.data.comment").value("Comment for " + status + " order"));

                // Verify comment was added
                Order updatedOrder = ordersRepository.findById(testOrder.getId()).orElseThrow();
                assertThat(updatedOrder.getComment()).isEqualTo("Comment for " + status + " order");
            }
        }

        @Test
        @DisplayName("Should successfully add maximum length comment")
        void shouldSuccessfullyAddMaximumLengthComment() throws Exception {
            // Arrange
            Order testOrder = createTestOrder(PENDING, null);
            String maxLengthComment = "A".repeat(500); // Maximum allowed length
            CommentRequest commentRequest = CommentRequest.builder()
                    .comment(maxLengthComment)
                    .build();
            
            String endpoint = baseEndpoint + "/" + testOrder.getId() + "/comment";

            // Act
            ResultActions result = mockMvc.perform(post(endpoint)
                    .with(user(authData.user().getEmail()))
                    .contentType(APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(commentRequest)));

            // Assert
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.comment").value(maxLengthComment));

            // Verify comment was added
            Order updatedOrder = ordersRepository.findById(testOrder.getId()).orElseThrow();
            assertThat(updatedOrder.getComment()).isEqualTo(maxLengthComment);
        }

        @Test
        @DisplayName("Should fail when comment exceeds maximum length")
        void shouldFailWhenCommentExceedsMaximumLength() throws Exception {
            // Arrange
            Order testOrder = createTestOrder(PENDING, null);
            String tooLongComment = "A".repeat(501); // Exceeds maximum length
            CommentRequest commentRequest = CommentRequest.builder()
                    .comment(tooLongComment)
                    .build();
            
            String endpoint = baseEndpoint + "/" + testOrder.getId() + "/comment";

            // Act
            ResultActions result = mockMvc.perform(post(endpoint)
                    .with(user(authData.user().getEmail()))
                    .contentType(APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(commentRequest)));

            // Assert
            result.andExpect(status().isBadRequest())
                    .andExpect(content().contentType("application/json"))
                    .andExpect(jsonPath("$.success").value(false));

            // Verify comment was not added
            Order unchangedOrder = ordersRepository.findById(testOrder.getId()).orElseThrow();
            assertThat(unchangedOrder.getComment()).isNull();
        }

        @Test
        @DisplayName("Should fail when comment is null")
        void shouldFailWhenCommentIsNull() throws Exception {
            // Arrange
            Order testOrder = createTestOrder(PENDING, null);
            CommentRequest commentRequest = CommentRequest.builder()
                    .comment(null)
                    .build();
            
            String endpoint = baseEndpoint + "/" + testOrder.getId() + "/comment";

            // Act
            ResultActions result = mockMvc.perform(post(endpoint)
                    .with(user(authData.user().getEmail()))
                    .contentType(APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(commentRequest)));

            // Assert
            result.andExpect(status().isBadRequest())
                    .andExpect(content().contentType("application/json"))
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("Should fail when order does not exist")
        void shouldFailWhenOrderDoesNotExist() throws Exception {
            // Arrange
            Long nonExistentOrderId = 99999L;
            CommentRequest commentRequest = CommentRequest.builder()
                    .comment("Comment for non-existent order")
                    .build();
            
            String endpoint = baseEndpoint + "/" + nonExistentOrderId + "/comment";

            // Act
            ResultActions result = mockMvc.perform(post(endpoint)
                    .with(user(authData.user().getEmail()))
                    .contentType(APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(commentRequest)));

            // Assert
            result.andExpect(status().isNotFound())
                    .andExpect(content().contentType("application/json"))
                    .andExpect(jsonPath("$.success").value(false));
        }
    }

    @Nested
    @DisplayName("PUT /v1/associations/{associationId}/orders/{orderId}/comment - Edit Comment")
    class EditCommentTests {

        @Test
        @DisplayName("Should successfully edit existing comment")
        void shouldSuccessfullyEditExistingComment() throws Exception {
            // Arrange
            Order testOrder = createTestOrder(PENDING, "Original comment");
            CommentRequest commentRequest = CommentRequest.builder()
                    .comment("Edited comment")
                    .build();
            
            String endpoint = baseEndpoint + "/" + testOrder.getId() + "/comment";

            // Act
            ResultActions result = mockMvc.perform(put(endpoint)
                    .with(user(authData.user().getEmail()))
                    .contentType(APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(commentRequest)));

            // Assert
            result.andExpect(status().isOk())
                    .andExpect(content().contentType("application/json"))
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Order comment edited successfully"))
                    .andExpect(jsonPath("$.data.comment").value("Edited comment"));

            // Verify comment was updated in database
            Order updatedOrder = ordersRepository.findById(testOrder.getId()).orElseThrow();
            assertThat(updatedOrder.getComment()).isEqualTo("Edited comment");
        }

        @Test
        @DisplayName("Should successfully edit comment on order without existing comment")
        void shouldSuccessfullyEditCommentOnOrderWithoutExistingComment() throws Exception {
            // Arrange
            Order testOrder = createTestOrder(PENDING, null);
            CommentRequest commentRequest = CommentRequest.builder()
                    .comment("New comment via edit")
                    .build();
            
            String endpoint = baseEndpoint + "/" + testOrder.getId() + "/comment";

            // Act
            ResultActions result = mockMvc.perform(put(endpoint)
                    .with(user(authData.user().getEmail()))
                    .contentType(APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(commentRequest)));

            // Assert
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Order comment edited successfully"))
                    .andExpect(jsonPath("$.data.comment").value("New comment via edit"));

            // Verify comment was added
            Order updatedOrder = ordersRepository.findById(testOrder.getId()).orElseThrow();
            assertThat(updatedOrder.getComment()).isEqualTo("New comment via edit");
        }

        @Test
        @DisplayName("Should fail when editing with invalid comment")
        void shouldFailWhenEditingWithInvalidComment() throws Exception {
            // Arrange
            Order testOrder = createTestOrder(PENDING, "Original comment");
            String tooLongComment = "A".repeat(501);
            CommentRequest commentRequest = CommentRequest.builder()
                    .comment(tooLongComment)
                    .build();
            
            String endpoint = baseEndpoint + "/" + testOrder.getId() + "/comment";

            // Act
            ResultActions result = mockMvc.perform(put(endpoint)
                    .with(user(authData.user().getEmail()))
                    .contentType(APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(commentRequest)));

            // Assert
            result.andExpect(status().isBadRequest())
                    .andExpect(content().contentType("application/json"))
                    .andExpect(jsonPath("$.success").value(false));

            // Verify original comment was not changed
            Order unchangedOrder = ordersRepository.findById(testOrder.getId()).orElseThrow();
            assertThat(unchangedOrder.getComment()).isEqualTo("Original comment");
        }

        @Test
        @DisplayName("Should fail when order does not exist")
        void shouldFailWhenOrderDoesNotExist() throws Exception {
            // Arrange
            Long nonExistentOrderId = 99999L;
            CommentRequest commentRequest = CommentRequest.builder()
                    .comment("Edit comment for non-existent order")
                    .build();
            
            String endpoint = baseEndpoint + "/" + nonExistentOrderId + "/comment";

            // Act
            ResultActions result = mockMvc.perform(put(endpoint)
                    .with(user(authData.user().getEmail()))
                    .contentType(APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(commentRequest)));

            // Assert
            result.andExpect(status().isNotFound())
                    .andExpect(content().contentType("application/json"))
                    .andExpect(jsonPath("$.success").value(false));
        }
    }

    @Nested
    @DisplayName("DELETE /v1/associations/{associationId}/orders/{orderId}/comment - Remove Comment")
    class RemoveCommentTests {

        @Test
        @DisplayName("Should successfully remove existing comment")
        void shouldSuccessfullyRemoveExistingComment() throws Exception {
            // Arrange
            Order testOrder = createTestOrder(PENDING, "Comment to be removed");
            String endpoint = baseEndpoint + "/" + testOrder.getId() + "/comment";

            // Act
            ResultActions result = mockMvc.perform(delete(endpoint)
                    .with(user(authData.user().getEmail())));

            // Assert
            result.andExpect(status().isNoContent());

            // Verify comment was removed from database
            Order updatedOrder = ordersRepository.findById(testOrder.getId()).orElseThrow();
            assertThat(updatedOrder.getComment()).isNull();
        }

        @Test
        @DisplayName("Should successfully handle removing comment from order without comment")
        void shouldSuccessfullyHandleRemovingCommentFromOrderWithoutComment() throws Exception {
            // Arrange
            Order testOrder = createTestOrder(PENDING, null);
            String endpoint = baseEndpoint + "/" + testOrder.getId() + "/comment";

            // Act
            ResultActions result = mockMvc.perform(delete(endpoint)
                    .with(user(authData.user().getEmail())));

            // Assert
            result.andExpect(status().isNoContent());

            // Verify comment remains null
            Order updatedOrder = ordersRepository.findById(testOrder.getId()).orElseThrow();
            assertThat(updatedOrder.getComment()).isNull();
        }

        @Test
        @DisplayName("Should successfully remove comment from orders with different statuses")
        void shouldSuccessfullyRemoveCommentFromOrdersWithDifferentStatuses() throws Exception {
            // Test removing comments from orders with different statuses
            OrderStatus[] statuses = {PENDING, COMPLETED, CANCELLED, UNPAID, REFUNDED};
            
            for (OrderStatus status : statuses) {
                // Arrange
                Order testOrder = createTestOrder(status, "Comment to remove from " + status + " order");
                String endpoint = baseEndpoint + "/" + testOrder.getId() + "/comment";

                // Act
                ResultActions result = mockMvc.perform(delete(endpoint)
                        .with(user(authData.user().getEmail())));

                // Assert
                result.andExpect(status().isNoContent());

                // Verify comment was removed
                Order updatedOrder = ordersRepository.findById(testOrder.getId()).orElseThrow();
                assertThat(updatedOrder.getComment()).isNull();
            }
        }

        @Test
        @DisplayName("Should fail when order does not exist")
        void shouldFailWhenOrderDoesNotExist() throws Exception {
            // Arrange
            Long nonExistentOrderId = 99999L;
            String endpoint = baseEndpoint + "/" + nonExistentOrderId + "/comment";

            // Act
            ResultActions result = mockMvc.perform(delete(endpoint)
                    .with(user(authData.user().getEmail())));

            // Assert
            result.andExpect(status().isNotFound())
                    .andExpect(content().contentType("application/json"))
                    .andExpect(jsonPath("$.success").value(false));
        }
    }

    @Nested
    @DisplayName("Comment Operations - Edge Cases and Integration")
    class EdgeCasesAndIntegrationTests {

        @Test
        @DisplayName("Should handle complete comment lifecycle (add, edit, remove)")
        void shouldHandleCompleteCommentLifecycle() throws Exception {
            // Arrange
            Order testOrder = createTestOrder(PENDING, null);
            String endpoint = baseEndpoint + "/" + testOrder.getId() + "/comment";

            // Step 1: Add comment
            CommentRequest addRequest = CommentRequest.builder()
                    .comment("Initial comment")
                    .build();

            mockMvc.perform(post(endpoint)
                    .with(user(authData.user().getEmail()))
                    .contentType(APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(addRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.comment").value("Initial comment"));

            // Verify comment was added
            Order afterAdd = ordersRepository.findById(testOrder.getId()).orElseThrow();
            assertThat(afterAdd.getComment()).isEqualTo("Initial comment");

            // Step 2: Edit comment
            CommentRequest editRequest = CommentRequest.builder()
                    .comment("Updated comment")
                    .build();

            mockMvc.perform(put(endpoint)
                    .with(user(authData.user().getEmail()))
                    .contentType(APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(editRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.comment").value("Updated comment"));

            // Verify comment was updated
            Order afterEdit = ordersRepository.findById(testOrder.getId()).orElseThrow();
            assertThat(afterEdit.getComment()).isEqualTo("Updated comment");

            // Step 3: Remove comment
            mockMvc.perform(delete(endpoint)
                    .with(user(authData.user().getEmail())))
                    .andExpect(status().isNoContent());

            // Verify comment was removed
            Order afterRemove = ordersRepository.findById(testOrder.getId()).orElseThrow();
            assertThat(afterRemove.getComment()).isNull();
        }

        @Test
        @DisplayName("Should handle special characters and whitespace in comments")
        void shouldHandleSpecialCharactersAndWhitespaceInComments() throws Exception {
            // Arrange
            Order testOrder = createTestOrder(PENDING, null);
            String specialComment = "Comment with special chars: !@#$%^&*()_+-=[]{}|;':\",./<>? and\nnewlines\tand\ttabs";
            CommentRequest commentRequest = CommentRequest.builder()
                    .comment(specialComment)
                    .build();
            
            String endpoint = baseEndpoint + "/" + testOrder.getId() + "/comment";

            // Act
            ResultActions result = mockMvc.perform(post(endpoint)
                    .with(user(authData.user().getEmail()))
                    .contentType(APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(commentRequest)));

            // Assert
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.comment").value(specialComment));

            // Verify comment was stored correctly
            Order updatedOrder = ordersRepository.findById(testOrder.getId()).orElseThrow();
            assertThat(updatedOrder.getComment()).isEqualTo(specialComment);
        }

        @Test
        @DisplayName("Should handle empty string comment")
        void shouldHandleEmptyStringComment() throws Exception {
            // Arrange
            Order testOrder = createTestOrder(PENDING, "Original comment");
            CommentRequest commentRequest = CommentRequest.builder()
                    .comment("")
                    .build();
            
            String endpoint = baseEndpoint + "/" + testOrder.getId() + "/comment";

            // Act
            ResultActions result = mockMvc.perform(post(endpoint)
                    .with(user(authData.user().getEmail()))
                    .contentType(APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(commentRequest)));

            // Assert
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.comment").value(""));

            // Verify empty comment was stored
            Order updatedOrder = ordersRepository.findById(testOrder.getId()).orElseThrow();
            assertThat(updatedOrder.getComment()).isEqualTo("");
        }

        @Test
        @DisplayName("Should handle concurrent comment operations")
        void shouldHandleConcurrentCommentOperations() throws Exception {
            // Arrange
            Order testOrder = createTestOrder(PENDING, null);
            String endpoint = baseEndpoint + "/" + testOrder.getId() + "/comment";

            // Simulate concurrent operations
            CommentRequest firstRequest = CommentRequest.builder()
                    .comment("First comment")
                    .build();

            CommentRequest secondRequest = CommentRequest.builder()
                    .comment("Second comment")
                    .build();

            // Act - Add first comment
            mockMvc.perform(post(endpoint)
                    .with(user(authData.user().getEmail()))
                    .contentType(APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(firstRequest)))
                    .andExpect(status().isOk());

            // Act - Immediately edit with second comment
            mockMvc.perform(put(endpoint)
                    .with(user(authData.user().getEmail()))
                    .contentType(APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(secondRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.comment").value("Second comment"));

            // Verify final state
            Order finalOrder = ordersRepository.findById(testOrder.getId()).orElseThrow();
            assertThat(finalOrder.getComment()).isEqualTo("Second comment");
        }
    }
} 