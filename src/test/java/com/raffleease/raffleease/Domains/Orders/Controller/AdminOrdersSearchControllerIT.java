package com.raffleease.raffleease.Domains.Orders.Controller;

import com.raffleease.raffleease.Base.AbstractIntegrationTest;
import com.raffleease.raffleease.Domains.Customers.Model.Customer;
import com.raffleease.raffleease.Domains.Customers.Repository.CustomersRepository;
import com.raffleease.raffleease.Domains.Orders.Model.Order;
import com.raffleease.raffleease.Domains.Orders.Model.OrderItem;
import com.raffleease.raffleease.Domains.Orders.Model.OrderStatus;
import com.raffleease.raffleease.Domains.Orders.Repository.OrdersRepository;
import com.raffleease.raffleease.Domains.Payments.Model.Payment;
import com.raffleease.raffleease.Domains.Payments.Model.PaymentMethod;
import com.raffleease.raffleease.Domains.Payments.Repository.PaymentsRepository;
import com.raffleease.raffleease.Domains.Raffles.Model.Raffle;
import com.raffleease.raffleease.Domains.Raffles.Model.RaffleStatistics;
import com.raffleease.raffleease.Domains.Raffles.Model.RaffleStatus;
import com.raffleease.raffleease.Domains.Raffles.Repository.RafflesRepository;
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
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.raffleease.raffleease.Domains.Orders.Model.OrderStatus.*;
import static com.raffleease.raffleease.Domains.Payments.Model.PaymentMethod.*;
import static java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME;
import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("Admin Orders Search Controller Integration Tests")
class AdminOrdersSearchControllerIT extends AbstractIntegrationTest {

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

    private AuthTestData authData;
    private String searchEndpoint;
    private List<Order> testOrders;
    private List<Raffle> testRaffles;
    private LocalDateTime baseTime;

    @BeforeEach
    void setUp() {
        authData = authTestUtils.createAuthenticatedUser();
        searchEndpoint = "/v1/associations/" + authData.association().getId() + "/orders";
        baseTime = LocalDateTime.now().minusDays(10);
        
        setupTestData();
    }

    private void setupTestData() {
        testRaffles = createTestRaffles();
        testOrders = createTestOrders();
    }

    private List<Raffle> createTestRaffles() {
        List<Raffle> raffles = new ArrayList<>();
        
        // Raffle 1: Summer Raffle
        RaffleStatistics stats1 = TestDataBuilder.statistics()
                .availableTickets(5L)
                .participants(2L)
                .totalOrders(2L)
                .pendingOrders(1L)
                .completedOrders(1L)
                .build();
        
        Raffle raffle1 = TestDataBuilder.raffle()
                .association(authData.association())
                .status(RaffleStatus.ACTIVE)
                .title("Summer Raffle 2024")
                .ticketPrice(BigDecimal.valueOf(15.00))
                .totalTickets(100L)
                .statistics(stats1)
                .build();
        raffles.add(rafflesRepository.save(raffle1));

        // Raffle 2: Winter Raffle
        RaffleStatistics stats2 = TestDataBuilder.statistics()
                .availableTickets(8L)
                .participants(1L)
                .totalOrders(1L)
                .pendingOrders(1L)
                .build();
        
        Raffle raffle2 = TestDataBuilder.raffle()
                .association(authData.association())
                .status(RaffleStatus.ACTIVE)
                .title("Winter Raffle 2024")
                .ticketPrice(BigDecimal.valueOf(25.00))
                .totalTickets(50L)
                .statistics(stats2)
                .build();
        raffles.add(rafflesRepository.save(raffle2));

        return raffles;
    }

    private List<Order> createTestOrders() {
        List<Order> orders = new ArrayList<>();
        
        // Order 1: PENDING order with CARD payment
        Customer customer1 = createCustomer("john smith", "john.smith@example.com", "+1234567890");
        Order order1 = createOrder(
                testRaffles.get(0), 
                customer1, 
                PENDING, 
                "ORD-PEND001",
                "First test order",
                baseTime.minusDays(5),
                null, null, null, null,
                BigDecimal.valueOf(45.00),
                CARD
        );
        orders.add(order1);

        // Order 2: COMPLETED order with PAYPAL payment
        Customer customer2 = createCustomer("jane doe", "jane.doe@example.com", "+1987654321");
        Order order2 = createOrder(
                testRaffles.get(0), 
                customer2, 
                COMPLETED, 
                "ORD-COMP002",
                "Second test order",
                baseTime.minusDays(3),
                baseTime.minusDays(2), null, null, null,
                BigDecimal.valueOf(30.00),
                PAYPAL
        );
        orders.add(order2);

        // Order 3: CANCELLED order with CASH payment
        Customer customer3 = createCustomer("bob johnson", "bob.johnson@example.com", "+1555123456");
        Order order3 = createOrder(
                testRaffles.get(1), 
                customer3, 
                CANCELLED, 
                "ORD-CANC003",
                null,
                baseTime.minusDays(7),
                null, baseTime.minusDays(6), null, null,
                BigDecimal.valueOf(75.00),
                CASH
        );
        orders.add(order3);

        // Order 4: UNPAID order with BANK_TRANSFER payment
        Customer customer4 = createCustomer("john doe", "john.doe@example.com", "+1234567890");
        Order order4 = createOrder(
                testRaffles.get(1), 
                customer4, 
                UNPAID, 
                "ORD-UNPA004",
                "Fourth test order",
                baseTime.minusDays(1),
                null, null, null, baseTime.minusHours(2),
                BigDecimal.valueOf(100.00),
                BANK_TRANSFER
        );
        orders.add(order4);

        // Order 5: REFUNDED order with VISA payment
        Customer customer5 = createCustomer("jane smith", "jane.smith@example.com", "+1987654321");
        Order order5 = createOrder(
                testRaffles.get(0), 
                customer5, 
                REFUNDED, 
                "ORD-REFN005",
                "Refunded order",
                baseTime.minusDays(8),
                baseTime.minusDays(7), null, baseTime.minusDays(6), null,
                BigDecimal.valueOf(60.00),
                VISA
        );
        orders.add(order5);

        return orders;
    }

    private Customer createCustomer(String fullName, String email, String phoneNumber) {
        String prefix = phoneNumber.substring(0, 2);
        String nationalNumber = phoneNumber.substring(2);
        
        Customer customer = TestDataBuilder.customer()
                .fullName(fullName)
                .email(email)
                .phoneNumber(prefix, nationalNumber)
                .build();
        return customersRepository.save(customer);
    }

    private Order createOrder(
            Raffle raffle, 
            Customer customer, 
            OrderStatus status, 
            String orderReference,
            String comment,
            LocalDateTime createdAt,
            LocalDateTime completedAt,
            LocalDateTime cancelledAt,
            LocalDateTime refundedAt,
            LocalDateTime unpaidAt,
            BigDecimal total,
            PaymentMethod paymentMethod
    ) {
        // Create order
        Order order = Order.builder()
                .raffle(raffle)
                .customer(customer)
                .status(status)
                .orderReference(orderReference)
                .comment(comment)
                .createdAt(createdAt)
                .completedAt(completedAt)
                .cancelledAt(cancelledAt)
                .refundedAt(refundedAt)
                .unpaidAt(unpaidAt)
                .orderItems(new ArrayList<>())
                .build();
        
        order = ordersRepository.save(order);

        // Create payment
        Payment payment = Payment.builder()
                .order(order)
                .total(total)
                .paymentMethod(paymentMethod)
                .build();
        payment = paymentsRepository.save(payment);
        
        order.setPayment(payment);
        
        // Create order items
        List<OrderItem> orderItems = new ArrayList<>();
        int ticketCount = total.divide(raffle.getTicketPrice()).intValue();
        
        for (int i = 1; i <= ticketCount; i++) {
            OrderItem item = OrderItem.builder()
                    .order(order)
                    .ticketNumber(String.valueOf(i))
                    .priceAtPurchase(raffle.getTicketPrice())
                    .ticketId((long) i)
                    .raffleId(raffle.getId())
                    .customerId(customer.getId())
                    .build();
            orderItems.add(item);
        }
        
        order.setOrderItems(orderItems);
        return ordersRepository.save(order);
    }

    @Nested
    @DisplayName("GET /v1/associations/{associationId}/orders - Basic Search")
    class BasicSearchTests {

        @Test
        @DisplayName("Should return all orders without filters")
        void shouldReturnAllOrdersWithoutFilters() throws Exception {
            // Act
            ResultActions result = mockMvc.perform(get(searchEndpoint)
                    .with(user(authData.user().getEmail())));

            // Assert
            result.andExpect(status().isOk())
                    .andExpect(content().contentType("application/json"))
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Orders retrieved successfully"))
                    .andExpect(jsonPath("$.data.content").isArray())
                    .andExpect(jsonPath("$.data.content", hasSize(5)))
                    .andExpect(jsonPath("$.data.totalElements").value(5))
                    .andExpect(jsonPath("$.data.totalPages").value(1))
                    .andExpect(jsonPath("$.data.first").value(true))
                    .andExpect(jsonPath("$.data.last").value(true));
        }

        @Test
        @DisplayName("Should return empty results for different association")
        void shouldReturnEmptyResultsForDifferentAssociation() throws Exception {
            // Arrange - Create another association with unique name
            AuthTestData otherAuthData = authTestUtils.createAuthenticatedUserWithCredentials(
                    "otheruser", 
                    "other@example.com", 
                    "password123"
            );
            String otherEndpoint = "/v1/associations/" + otherAuthData.association().getId() + "/orders";

            // Act
            ResultActions result = mockMvc.perform(get(otherEndpoint)
                    .with(user(otherAuthData.user().getEmail())));

            // Assert
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.content").isArray())
                    .andExpect(jsonPath("$.data.content", hasSize(0)))
                    .andExpect(jsonPath("$.data.totalElements").value(0));
        }
    }

    @Nested
    @DisplayName("GET /v1/associations/{associationId}/orders - Status Filter")
    class StatusFilterTests {

        @Test
        @DisplayName("Should filter orders by PENDING status")
        void shouldFilterOrdersByPendingStatus() throws Exception {
            // Act
            ResultActions result = mockMvc.perform(get(searchEndpoint)
                    .param("status", "PENDING")
                    .with(user(authData.user().getEmail())));

            // Assert
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content", hasSize(1)))
                    .andExpect(jsonPath("$.data.content[0].status").value("PENDING"))
                    .andExpect(jsonPath("$.data.content[0].orderReference").value("ORD-PEND001"));
        }

        @Test
        @DisplayName("Should filter orders by COMPLETED status")
        void shouldFilterOrdersByCompletedStatus() throws Exception {
            // Act
            ResultActions result = mockMvc.perform(get(searchEndpoint)
                    .param("status", "COMPLETED")
                    .with(user(authData.user().getEmail())));

            // Assert
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content", hasSize(1)))
                    .andExpect(jsonPath("$.data.content[0].status").value("COMPLETED"))
                    .andExpect(jsonPath("$.data.content[0].orderReference").value("ORD-COMP002"));
        }

        @Test
        @DisplayName("Should filter orders by CANCELLED status")
        void shouldFilterOrdersByCancelledStatus() throws Exception {
            // Act
            ResultActions result = mockMvc.perform(get(searchEndpoint)
                    .param("status", "CANCELLED")
                    .with(user(authData.user().getEmail())));

            // Assert
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content", hasSize(1)))
                    .andExpect(jsonPath("$.data.content[0].status").value("CANCELLED"))
                    .andExpect(jsonPath("$.data.content[0].orderReference").value("ORD-CANC003"));
        }
    }

    @Nested
    @DisplayName("GET /v1/associations/{associationId}/orders - Payment Method Filter")
    class PaymentMethodFilterTests {

        @Test
        @DisplayName("Should filter orders by CARD payment method")
        void shouldFilterOrdersByCardPaymentMethod() throws Exception {
            // Act
            ResultActions result = mockMvc.perform(get(searchEndpoint)
                    .param("paymentMethod", "CARD")
                    .with(user(authData.user().getEmail())));

            // Assert
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content", hasSize(1)))
                    .andExpect(jsonPath("$.data.content[0].payment.paymentMethod").value("card"))
                    .andExpect(jsonPath("$.data.content[0].orderReference").value("ORD-PEND001"));
        }

        @Test
        @DisplayName("Should filter orders by PAYPAL payment method")
        void shouldFilterOrdersByPaypalPaymentMethod() throws Exception {
            // Act
            ResultActions result = mockMvc.perform(get(searchEndpoint)
                    .param("paymentMethod", "PAYPAL")
                    .with(user(authData.user().getEmail())));

            // Assert
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content", hasSize(1)))
                    .andExpect(jsonPath("$.data.content[0].payment.paymentMethod").value("paypal"))
                    .andExpect(jsonPath("$.data.content[0].orderReference").value("ORD-COMP002"));
        }
    }

    @Nested
    @DisplayName("GET /v1/associations/{associationId}/orders - Order Reference Filter")
    class OrderReferenceFilterTests {

        @Test
        @DisplayName("Should filter orders by exact order reference")
        void shouldFilterOrdersByExactOrderReference() throws Exception {
            // Act
            ResultActions result = mockMvc.perform(get(searchEndpoint)
                    .param("orderReference", "ORD-PEND001")
                    .with(user(authData.user().getEmail())));

            // Assert
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content", hasSize(1)))
                    .andExpect(jsonPath("$.data.content[0].orderReference").value("ORD-PEND001"));
        }

        @Test
        @DisplayName("Should filter orders by partial order reference")
        void shouldFilterOrdersByPartialOrderReference() throws Exception {
            // Act
            ResultActions result = mockMvc.perform(get(searchEndpoint)
                    .param("orderReference", "PEND")
                    .with(user(authData.user().getEmail())));

            // Assert
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content", hasSize(1)))
                    .andExpect(jsonPath("$.data.content[0].orderReference").value("ORD-PEND001"));
        }

        @Test
        @DisplayName("Should return empty results for non-existent order reference")
        void shouldReturnEmptyResultsForNonExistentOrderReference() throws Exception {
            // Act
            ResultActions result = mockMvc.perform(get(searchEndpoint)
                    .param("orderReference", "NON-EXISTENT")
                    .with(user(authData.user().getEmail())));

            // Assert
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content", hasSize(0)));
        }
    }

    @Nested
    @DisplayName("GET /v1/associations/{associationId}/orders - Customer Filters")
    class CustomerFilterTests {

        @Test
        @DisplayName("Should filter orders by customer name")
        void shouldFilterOrdersByCustomerName() throws Exception {
            // Act
            ResultActions result = mockMvc.perform(get(searchEndpoint)
                    .param("customerName", "john")
                    .with(user(authData.user().getEmail())));

            // Assert
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content", hasSize(3)))
                    .andExpect(jsonPath("$.data.content[*].customer.fullName", everyItem(containsString("john"))));
        }

        @Test
        @DisplayName("Should filter orders by customer email")
        void shouldFilterOrdersByCustomerEmail() throws Exception {
            // Act
            ResultActions result = mockMvc.perform(get(searchEndpoint)
                    .param("customerEmail", "jane")
                    .with(user(authData.user().getEmail())));

            // Assert
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content", hasSize(2)))
                    .andExpect(jsonPath("$.data.content[*].customer.email", everyItem(containsString("jane"))));
        }

        @Test
        @DisplayName("Should filter orders by customer phone")
        void shouldFilterOrdersByCustomerPhone() throws Exception {
            // Act
            ResultActions result = mockMvc.perform(get(searchEndpoint)
                    .param("customerPhone", "1234567890")
                    .with(user(authData.user().getEmail())));

            // Assert
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content", hasSize(2)))
                    .andExpect(jsonPath("$.data.content[*].customer.phoneNumber.prefix", everyItem(containsString("+1"))))
                    .andExpect(jsonPath("$.data.content[*].customer.phoneNumber.nationalNumber", everyItem(containsString("234567890"))));
        }
    }

    @Nested
    @DisplayName("GET /v1/associations/{associationId}/orders - Raffle Filter")
    class RaffleFilterTests {

        @Test
        @DisplayName("Should filter orders by raffle ID")
        void shouldFilterOrdersByRaffleId() throws Exception {
            // Act
            ResultActions result = mockMvc.perform(get(searchEndpoint)
                    .param("raffleId", testRaffles.get(0).getId().toString())
                    .with(user(authData.user().getEmail())));

            // Assert
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content", hasSize(3)))
                    .andExpect(jsonPath("$.data.content[*].orderItems[0].raffleId", everyItem(is(testRaffles.get(0).getId().intValue()))));
        }

        @Test
        @DisplayName("Should return empty results for non-existent raffle ID")
        void shouldReturnEmptyResultsForNonExistentRaffleId() throws Exception {
            // Act
            ResultActions result = mockMvc.perform(get(searchEndpoint)
                    .param("raffleId", "99999")
                    .with(user(authData.user().getEmail())));

            // Assert
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content", hasSize(0)));
        }
    }

    @Nested
    @DisplayName("GET /v1/associations/{associationId}/orders - Total Amount Filters")
    class TotalAmountFilterTests {

        @Test
        @DisplayName("Should filter orders by minimum total")
        void shouldFilterOrdersByMinimumTotal() throws Exception {
            // Act
            ResultActions result = mockMvc.perform(get(searchEndpoint)
                    .param("minTotal", "50.00")
                    .with(user(authData.user().getEmail())));

            // Assert
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content", hasSize(3)))
                    .andExpect(jsonPath("$.data.content[*].payment.total", everyItem(greaterThanOrEqualTo(50.0))));
        }

        @Test
        @DisplayName("Should filter orders by maximum total")
        void shouldFilterOrdersByMaximumTotal() throws Exception {
            // Act
            ResultActions result = mockMvc.perform(get(searchEndpoint)
                    .param("maxTotal", "50.00")
                    .with(user(authData.user().getEmail())));

            // Assert
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content", hasSize(2)))
                    .andExpect(jsonPath("$.data.content[*].payment.total", everyItem(lessThanOrEqualTo(50.0))));
        }

        @Test
        @DisplayName("Should filter orders by total range")
        void shouldFilterOrdersByTotalRange() throws Exception {
            // Act
            ResultActions result = mockMvc.perform(get(searchEndpoint)
                    .param("minTotal", "40.00")
                    .param("maxTotal", "80.00")
                    .with(user(authData.user().getEmail())));

            // Assert
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content", hasSize(3)))
                    .andExpect(jsonPath("$.data.content[*].payment.total", everyItem(allOf(
                            greaterThanOrEqualTo(40.0),
                            lessThanOrEqualTo(80.0)
                    ))));
        }
    }

    @Nested
    @DisplayName("GET /v1/associations/{associationId}/orders - Date Range Filters")
    class DateRangeFilterTests {

        @Test
        @DisplayName("Should filter orders by creation date range")
        void shouldFilterOrdersByCreationDateRange() throws Exception {
            // Arrange - Use a range that includes the current time since @CreationTimestamp sets createdAt to now
            LocalDateTime now = LocalDateTime.now();
            String fromDate = now.minusMinutes(1).format(ISO_LOCAL_DATE_TIME);
            String toDate = now.plusMinutes(1).format(ISO_LOCAL_DATE_TIME);

            // Act
            ResultActions result = mockMvc.perform(get(searchEndpoint)
                    .param("createdFrom", fromDate)
                    .param("createdTo", toDate)
                    .with(user(authData.user().getEmail())));

            // Assert - All orders should be included since they were all created "now"
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content", hasSize(5)));
        }

        @Test
        @DisplayName("Should filter orders by completion date range")
        void shouldFilterOrdersByCompletionDateRange() throws Exception {
            // Arrange
            String fromDate = baseTime.minusDays(8).format(ISO_LOCAL_DATE_TIME);
            String toDate = baseTime.minusDays(1).format(ISO_LOCAL_DATE_TIME);

            // Act
            ResultActions result = mockMvc.perform(get(searchEndpoint)
                    .param("completedFrom", fromDate)
                    .param("completedTo", toDate)
                    .with(user(authData.user().getEmail())));

            // Assert
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content", hasSize(2)));
        }

        @Test
        @DisplayName("Should filter orders by cancellation date range")
        void shouldFilterOrdersByCancellationDateRange() throws Exception {
            // Arrange
            String fromDate = baseTime.minusDays(7).format(ISO_LOCAL_DATE_TIME);
            String toDate = baseTime.minusDays(5).format(ISO_LOCAL_DATE_TIME);

            // Act
            ResultActions result = mockMvc.perform(get(searchEndpoint)
                    .param("cancelledFrom", fromDate)
                    .param("cancelledTo", toDate)
                    .with(user(authData.user().getEmail())));

            // Assert
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content", hasSize(1)))
                    .andExpect(jsonPath("$.data.content[0].status").value("CANCELLED"));
        }
    }

    @Nested
    @DisplayName("GET /v1/associations/{associationId}/orders - Combined Filters")
    class CombinedFiltersTests {

        @Test
        @DisplayName("Should filter orders by status and payment method")
        void shouldFilterOrdersByStatusAndPaymentMethod() throws Exception {
            // Act
            ResultActions result = mockMvc.perform(get(searchEndpoint)
                    .param("status", "COMPLETED")
                    .param("paymentMethod", "PAYPAL")
                    .with(user(authData.user().getEmail())));

            // Assert
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content", hasSize(1)))
                    .andExpect(jsonPath("$.data.content[0].status").value("COMPLETED"))
                    .andExpect(jsonPath("$.data.content[0].payment.paymentMethod").value("paypal"));
        }

        @Test
        @DisplayName("Should filter orders by customer and total range")
        void shouldFilterOrdersByCustomerAndTotalRange() throws Exception {
            // Act
            ResultActions result = mockMvc.perform(get(searchEndpoint)
                    .param("customerName", "john")
                    .param("minTotal", "40.00")
                    .param("maxTotal", "60.00")
                    .with(user(authData.user().getEmail())));

            // Assert
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content", hasSize(1)))
                    .andExpect(jsonPath("$.data.content[0].customer.fullName", containsString("john")))
                    .andExpect(jsonPath("$.data.content[0].payment.total", allOf(
                            greaterThanOrEqualTo(40.0),
                            lessThanOrEqualTo(60.0)
                    )));
        }

        @Test
        @DisplayName("Should return empty results when filters don't match")
        void shouldReturnEmptyResultsWhenFiltersDoNotMatch() throws Exception {
            // Act
            ResultActions result = mockMvc.perform(get(searchEndpoint)
                    .param("status", "PENDING")
                    .param("paymentMethod", "PAYPAL")
                    .with(user(authData.user().getEmail())));

            // Assert
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content", hasSize(0)));
        }
    }

    @Nested
    @DisplayName("GET /v1/associations/{associationId}/orders - Pagination and Sorting")
    class PaginationAndSortingTests {

        @Test
        @DisplayName("Should paginate results correctly")
        void shouldPaginateResultsCorrectly() throws Exception {
            // Act
            ResultActions result = mockMvc.perform(get(searchEndpoint)
                    .param("page", "0")
                    .param("size", "2")
                    .with(user(authData.user().getEmail())));

            // Assert
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content", hasSize(2)))
                    .andExpect(jsonPath("$.data.totalElements").value(5))
                    .andExpect(jsonPath("$.data.totalPages").value(3))
                    .andExpect(jsonPath("$.data.first").value(true))
                    .andExpect(jsonPath("$.data.last").value(false))
                    .andExpect(jsonPath("$.data.number").value(0))
                    .andExpect(jsonPath("$.data.size").value(2));
        }

        @Test
        @DisplayName("Should return correct page when requested")
        void shouldReturnCorrectPageWhenRequested() throws Exception {
            // Act
            ResultActions result = mockMvc.perform(get(searchEndpoint)
                    .param("page", "1")
                    .param("size", "2")
                    .with(user(authData.user().getEmail())));

            // Assert
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content", hasSize(2)))
                    .andExpect(jsonPath("$.data.number").value(1))
                    .andExpect(jsonPath("$.data.first").value(false))
                    .andExpect(jsonPath("$.data.last").value(false));
        }

        @Test
        @DisplayName("Should sort orders by creation date descending by default")
        void shouldSortOrdersByCreationDateDescendingByDefault() throws Exception {
            // Act
            ResultActions result = mockMvc.perform(get(searchEndpoint)
                    .with(user(authData.user().getEmail())));

            // Assert
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content[0].orderReference").value("ORD-REFN005"))
                    .andExpect(jsonPath("$.data.content[1].orderReference").value("ORD-UNPA004"));
        }
    }

    @Nested
    @DisplayName("GET /v1/associations/{associationId}/orders - Edge Cases")
    class EdgeCasesTests {

        @Test
        @DisplayName("Should handle empty string filters gracefully")
        void shouldHandleEmptyStringFiltersGracefully() throws Exception {
            // Act
            ResultActions result = mockMvc.perform(get(searchEndpoint)
                    .param("orderReference", "")
                    .param("customerName", "")
                    .param("customerEmail", "")
                    .with(user(authData.user().getEmail())));

            // Assert
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content", hasSize(5)));
        }

        @Test
        @DisplayName("Should handle null date filters gracefully")
        void shouldHandleNullDateFiltersGracefully() throws Exception {
            // Act
            ResultActions result = mockMvc.perform(get(searchEndpoint)
                    .param("createdFrom", "")
                    .param("createdTo", "")
                    .with(user(authData.user().getEmail())));

            // Assert
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content", hasSize(5)));
        }

        @Test
        @DisplayName("Should handle zero amounts gracefully")
        void shouldHandleZeroAmountsGracefully() throws Exception {
            // Act
            ResultActions result = mockMvc.perform(get(searchEndpoint)
                    .param("minTotal", "0")
                    .param("maxTotal", "0")
                    .with(user(authData.user().getEmail())));

            // Assert
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content", hasSize(0)));
        }

        @Test
        @DisplayName("Should return empty results when no orders match criteria")
        void shouldReturnEmptyResultsWhenNoOrdersMatchCriteria() throws Exception {
            // Act
            ResultActions result = mockMvc.perform(get(searchEndpoint)
                    .param("customerName", "NonExistentCustomer")
                    .with(user(authData.user().getEmail())));

            // Assert
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content", hasSize(0)))
                    .andExpect(jsonPath("$.data.totalElements").value(0))
                    .andExpect(jsonPath("$.data.totalPages").value(0));
        }
    }
} 