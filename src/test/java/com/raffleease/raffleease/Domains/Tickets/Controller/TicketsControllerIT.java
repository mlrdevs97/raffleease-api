package com.raffleease.raffleease.Domains.Tickets.Controller;

import com.raffleease.raffleease.Base.AbstractIntegrationTest;
import com.raffleease.raffleease.Domains.Customers.Model.Customer;
import com.raffleease.raffleease.Domains.Customers.Repository.CustomersRepository;
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
import java.util.ArrayList;
import java.util.List;

import static com.raffleease.raffleease.Domains.Tickets.Model.TicketStatus.*;
import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("Tickets Controller Integration Tests")
class TicketsControllerIT extends AbstractIntegrationTest {

    @Autowired
    private AuthTestUtils authTestUtils;

    @Autowired
    private TicketsRepository ticketsRepository;

    @Autowired
    private RafflesRepository rafflesRepository;

    @Autowired
    private CustomersRepository customersRepository;

    private AuthTestData authData;
    private String searchEndpoint;
    private String randomEndpoint;
    private Raffle testRaffle;
    private List<Ticket> testTickets;
    private List<Customer> testCustomers;

    @BeforeEach
    void setUp() {
        authData = authTestUtils.createAuthenticatedUser();
        setupTestData();
        searchEndpoint = String.format("/v1/associations/%d/raffles/%d/tickets", 
                authData.association().getId(), testRaffle.getId());
        randomEndpoint = String.format("/v1/associations/%d/raffles/%d/tickets/random", 
                authData.association().getId(), testRaffle.getId());
    }

    private void setupTestData() {
        testRaffle = createTestRaffle();
        testCustomers = createTestCustomers();
        testTickets = createTestTickets();
    }

    private Raffle createTestRaffle() {
        RaffleStatistics stats = TestDataBuilder.statistics()
                .availableTickets(50L)
                .participants(3L)
                .totalOrders(5L)
                .build();

        Raffle raffle = TestDataBuilder.raffle()
                .association(authData.association())
                .status(RaffleStatus.ACTIVE)
                .title("Test Raffle for Tickets")
                .ticketPrice(BigDecimal.valueOf(10.00))
                .totalTickets(100L)
                .statistics(stats)
                .build();

        return rafflesRepository.save(raffle);
    }

    private List<Customer> createTestCustomers() {
        List<Customer> customers = new ArrayList<>();
        
        Customer customer1 = customersRepository.save(TestDataBuilder.customer()
                .fullName("John Smith")
                .email("john.smith@example.com")
                .phoneNumber("+1", "234567890")
                .build());
        customers.add(customer1);

        Customer customer2 = customersRepository.save(TestDataBuilder.customer()
                .fullName("Jane Doe")
                .email("jane.doe@example.com")
                .phoneNumber("+1", "987654321")
                .build());
        customers.add(customer2);

        Customer customer3 = customersRepository.save(TestDataBuilder.customer()
                .fullName("Bob Johnson")
                .email("bob.johnson@example.com")
                .phoneNumber("+1", "555123456")
                .build());
        customers.add(customer3);

        return customers;
    }

    private List<Ticket> createTestTickets() {
        List<Ticket> tickets = new ArrayList<>();

        // Available tickets
        for (int i = 1; i <= 10; i++) {
            Ticket ticket = TestDataBuilder.ticket()
                    .raffle(testRaffle)
                    .ticketNumber("TICKET-" + String.format("%03d", i))
                    .status(AVAILABLE)
                    .build();
            tickets.add(ticket);
        }

        // Reserved tickets
        for (int i = 11; i <= 15; i++) {
            Ticket ticket = TestDataBuilder.ticket()
                    .raffle(testRaffle)
                    .ticketNumber("TICKET-" + String.format("%03d", i))
                    .status(RESERVED)
                    .customer(testCustomers.get(0))
                    .build();
            tickets.add(ticket);
        }

        // Sold tickets
        for (int i = 16; i <= 20; i++) {
            Ticket ticket = TestDataBuilder.ticket()
                    .raffle(testRaffle)
                    .ticketNumber("TICKET-" + String.format("%03d", i))
                    .status(SOLD)
                    .customer(testCustomers.get(1))
                    .build();
            tickets.add(ticket);
        }

        return ticketsRepository.saveAll(tickets);
    }

    @Nested
    @DisplayName("GET /v1/associations/{associationId}/raffles/{raffleId}/tickets - Basic Search")
    class BasicSearchTests {

        @Test
        @DisplayName("Should return all tickets without filters")
        void shouldReturnAllTicketsWithoutFilters() throws Exception {
            // Act
            ResultActions result = mockMvc.perform(get(searchEndpoint)
                    .with(user(authData.user().getEmail())));

            // Assert
            result.andExpect(status().isOk())
                    .andExpect(content().contentType("application/json"))
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Ticket retrieved successfully"))
                    .andExpect(jsonPath("$.data.content").isArray())
                    .andExpect(jsonPath("$.data.content", hasSize(20)))
                    .andExpect(jsonPath("$.data.totalElements").value(20))
                    .andExpect(jsonPath("$.data.totalPages").value(1))
                    .andExpect(jsonPath("$.data.first").value(true))
                    .andExpect(jsonPath("$.data.last").value(true));
        }

        @Test
        @DisplayName("Should return empty results for different association")
        void shouldReturnEmptyResultsForDifferentAssociation() throws Exception {
            // Arrange
            AuthTestData otherAuthData = authTestUtils.createAuthenticatedUserWithCredentials(
                    "otheruser", 
                    "other@example.com", 
                    "password123"
            );
            String otherEndpoint = String.format("/v1/associations/%d/raffles/%d/tickets", 
                    otherAuthData.association().getId(), testRaffle.getId());

            // Act
            ResultActions result = mockMvc.perform(get(otherEndpoint)
                    .with(user(otherAuthData.user().getEmail())));

            // Assert
            result.andExpect(status().isOk())
                    .andExpect(content().contentType("application/json"))
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Ticket retrieved successfully"))
                    .andExpect(jsonPath("$.data.content").isArray())
                    .andExpect(jsonPath("$.data.content", hasSize(0)))
                    .andExpect(jsonPath("$.data.totalElements").value(0));
        }

        @Test
        @DisplayName("Should return empty results for non-existent raffle")
        void shouldReturnEmptyResultsForNonExistentRaffle() throws Exception {
            // Arrange
            String nonExistentRaffleEndpoint = String.format("/v1/associations/%d/raffles/%d/tickets", 
                    authData.association().getId(), 999999L);

            // Act
            ResultActions result = mockMvc.perform(get(nonExistentRaffleEndpoint)
                    .with(user(authData.user().getEmail())));

            // Assert
            result.andExpect(status().isOk())
                    .andExpect(content().contentType("application/json"))
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Ticket retrieved successfully"))
                    .andExpect(jsonPath("$.data.content").isArray())
                    .andExpect(jsonPath("$.data.content", hasSize(0)))
                    .andExpect(jsonPath("$.data.totalElements").value(0));
        }

        @Test
        @DisplayName("Should return 401 when not authenticated")
        void shouldReturn401WhenNotAuthenticated() throws Exception {
            // Act
            ResultActions result = mockMvc.perform(get(searchEndpoint));

            // Assert
            result.andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("GET /v1/associations/{associationId}/raffles/{raffleId}/tickets - Status Filter")
    class StatusFilterTests {

        @Test
        @DisplayName("Should filter tickets by AVAILABLE status")
        void shouldFilterTicketsByAvailableStatus() throws Exception {
            // Act
            ResultActions result = mockMvc.perform(get(searchEndpoint)
                    .param("status", "AVAILABLE")
                    .with(user(authData.user().getEmail())));

            // Assert
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content", hasSize(10)))
                    .andExpect(jsonPath("$.data.content[*].status", everyItem(is("AVAILABLE"))))
                    .andExpect(jsonPath("$.data.totalElements").value(10));
        }

        @Test
        @DisplayName("Should filter tickets by RESERVED status")
        void shouldFilterTicketsByReservedStatus() throws Exception {
            // Act
            ResultActions result = mockMvc.perform(get(searchEndpoint)
                    .param("status", "RESERVED")
                    .with(user(authData.user().getEmail())));

            // Assert
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content", hasSize(5)))
                    .andExpect(jsonPath("$.data.content[*].status", everyItem(is("RESERVED"))))
                    .andExpect(jsonPath("$.data.totalElements").value(5));
        }

        @Test
        @DisplayName("Should filter tickets by SOLD status")
        void shouldFilterTicketsBySoldStatus() throws Exception {
            // Act
            ResultActions result = mockMvc.perform(get(searchEndpoint)
                    .param("status", "SOLD")
                    .with(user(authData.user().getEmail())));

            // Assert
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content", hasSize(5)))
                    .andExpect(jsonPath("$.data.content[*].status", everyItem(is("SOLD"))))
                    .andExpect(jsonPath("$.data.totalElements").value(5));
        }
    }

    @Nested
    @DisplayName("GET /v1/associations/{associationId}/raffles/{raffleId}/tickets - Ticket Number Filter")
    class TicketNumberFilterTests {

        @Test
        @DisplayName("Should filter tickets by exact ticket number")
        void shouldFilterTicketsByExactTicketNumber() throws Exception {
            // Act
            ResultActions result = mockMvc.perform(get(searchEndpoint)
                    .param("ticketNumber", "TICKET-001")
                    .with(user(authData.user().getEmail())));

            // Assert
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content", hasSize(1)))
                    .andExpect(jsonPath("$.data.content[0].ticketNumber").value("TICKET-001"));
        }

        @Test
        @DisplayName("Should filter tickets by partial ticket number")
        void shouldFilterTicketsByPartialTicketNumber() throws Exception {
            // Act
            ResultActions result = mockMvc.perform(get(searchEndpoint)
                    .param("ticketNumber", "001")
                    .with(user(authData.user().getEmail())));

            // Assert
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content", hasSize(1)))
                    .andExpect(jsonPath("$.data.content[0].ticketNumber").value("TICKET-001"));
        }

        @Test
        @DisplayName("Should return empty results for non-existent ticket number")
        void shouldReturnEmptyResultsForNonExistentTicketNumber() throws Exception {
            // Act
            ResultActions result = mockMvc.perform(get(searchEndpoint)
                    .param("ticketNumber", "NON-EXISTENT")
                    .with(user(authData.user().getEmail())));

            // Assert
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content", hasSize(0)))
                    .andExpect(jsonPath("$.data.totalElements").value(0));
        }
    }

    @Nested
    @DisplayName("GET /v1/associations/{associationId}/raffles/{raffleId}/tickets - Customer Filter")
    class CustomerFilterTests {

        @Test
        @DisplayName("Should filter tickets by customer ID")
        void shouldFilterTicketsByCustomerId() throws Exception {
            // Act
            ResultActions result = mockMvc.perform(get(searchEndpoint)
                    .param("customerId", testCustomers.get(0).getId().toString())
                    .with(user(authData.user().getEmail())));

            // Assert
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content", hasSize(5)))
                    .andExpect(jsonPath("$.data.content[*].customer.id", everyItem(is(testCustomers.get(0).getId().intValue()))));
        }

        @Test
        @DisplayName("Should return empty results for non-existent customer ID")
        void shouldReturnEmptyResultsForNonExistentCustomerId() throws Exception {
            // Act
            ResultActions result = mockMvc.perform(get(searchEndpoint)
                    .param("customerId", "99999")
                    .with(user(authData.user().getEmail())));

            // Assert
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content", hasSize(0)));
        }
    }

    @Nested
    @DisplayName("GET /v1/associations/{associationId}/raffles/{raffleId}/tickets - Combined Filters")
    class CombinedFiltersTests {

        @Test
        @DisplayName("Should filter tickets by status and customer ID")
        void shouldFilterTicketsByStatusAndCustomerId() throws Exception {
            // Act
            ResultActions result = mockMvc.perform(get(searchEndpoint)
                    .param("status", "RESERVED")
                    .param("customerId", testCustomers.get(0).getId().toString())
                    .with(user(authData.user().getEmail())));

            // Assert
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content", hasSize(5)))
                    .andExpect(jsonPath("$.data.content[*].status", everyItem(is("RESERVED"))))
                    .andExpect(jsonPath("$.data.content[*].customer.id", everyItem(is(testCustomers.get(0).getId().intValue()))));
        }

        @Test
        @DisplayName("Should return empty results when combined filters don't match")
        void shouldReturnEmptyResultsWhenCombinedFiltersDoNotMatch() throws Exception {
            // Act
            ResultActions result = mockMvc.perform(get(searchEndpoint)
                    .param("status", "AVAILABLE")
                    .param("customerId", testCustomers.get(0).getId().toString())
                    .with(user(authData.user().getEmail())));

            // Assert
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content", hasSize(0)));
        }
    }

    @Nested
    @DisplayName("GET /v1/associations/{associationId}/raffles/{raffleId}/tickets - Pagination and Sorting")
    class PaginationAndSortingTests {

        @Test
        @DisplayName("Should paginate results correctly")
        void shouldPaginateResultsCorrectly() throws Exception {
            // Act
            ResultActions result = mockMvc.perform(get(searchEndpoint)
                    .param("page", "0")
                    .param("size", "10")
                    .with(user(authData.user().getEmail())));

            // Assert
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content", hasSize(10)))
                    .andExpect(jsonPath("$.data.totalElements").value(20))
                    .andExpect(jsonPath("$.data.totalPages").value(2))
                    .andExpect(jsonPath("$.data.first").value(true))
                    .andExpect(jsonPath("$.data.last").value(false))
                    .andExpect(jsonPath("$.data.number").value(0))
                    .andExpect(jsonPath("$.data.size").value(10));
        }

        @Test
        @DisplayName("Should return correct page when requested")
        void shouldReturnCorrectPageWhenRequested() throws Exception {
            // Act
            ResultActions result = mockMvc.perform(get(searchEndpoint)
                    .param("page", "1")
                    .param("size", "10")
                    .with(user(authData.user().getEmail())));

            // Assert
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content", hasSize(10)))
                    .andExpect(jsonPath("$.data.number").value(1))
                    .andExpect(jsonPath("$.data.first").value(false))
                    .andExpect(jsonPath("$.data.last").value(true));
        }

        @Test
        @DisplayName("Should sort tickets by creation date descending by default")
        void shouldSortTicketsByCreationDateDescendingByDefault() throws Exception {
            // Act
            ResultActions result = mockMvc.perform(get(searchEndpoint)
                    .param("size", "5")
                    .with(user(authData.user().getEmail())));

            // Assert
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content", hasSize(5)));
        }
    }

    @Nested
    @DisplayName("GET /v1/associations/{associationId}/raffles/{raffleId}/tickets/random - Random Tickets")
    class RandomTicketsTests {

        @Test
        @DisplayName("Should successfully get random tickets when enough available")
        void shouldGetRandomTicketsWhenEnoughAvailable() throws Exception {
            // Act
            ResultActions result = mockMvc.perform(get(randomEndpoint)
                    .param("quantity", "5")
                    .with(user(authData.user().getEmail())));

            // Assert
            result.andExpect(status().isOk())
                    .andExpect(content().contentType("application/json"))
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Random tickets retrieved successfully"))
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data", hasSize(5)))
                    .andExpect(jsonPath("$.data[*].status", everyItem(is("AVAILABLE"))));
        }

        @Test
        @DisplayName("Should successfully get all available tickets when quantity equals available count")
        void shouldGetAllAvailableTicketsWhenQuantityEqualsAvailableCount() throws Exception {
            // Act
            ResultActions result = mockMvc.perform(get(randomEndpoint)
                    .param("quantity", "10")
                    .with(user(authData.user().getEmail())));

            // Assert
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data", hasSize(10)))
                    .andExpect(jsonPath("$.data[*].status", everyItem(is("AVAILABLE"))));
        }

        @Test
        @DisplayName("Should return 400 when requesting more tickets than available")
        void shouldReturn400WhenRequestingMoreTicketsThanAvailable() throws Exception {
            // Act
            ResultActions result = mockMvc.perform(get(randomEndpoint)
                    .param("quantity", "15")
                    .with(user(authData.user().getEmail())));

            // Assert
            result.andExpect(status().isBadRequest())
                    .andExpect(content().contentType("application/json"))
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("Not enough tickets were found for this order"));
        }

        @Test
        @DisplayName("Should return empty array when quantity is zero")
        void shouldReturnEmptyArrayWhenQuantityIsZero() throws Exception {
            // Act
            ResultActions result = mockMvc.perform(get(randomEndpoint)
                    .param("quantity", "0")
                    .with(user(authData.user().getEmail())));

            // Assert
            result.andExpect(status().isOk())
                    .andExpect(content().contentType("application/json"))
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Random tickets retrieved successfully"))
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data", hasSize(0)));
        }

        @Test
        @DisplayName("Should return 400 when quantity parameter is missing")
        void shouldReturn400WhenQuantityParameterIsMissing() throws Exception {
            // Act
            ResultActions result = mockMvc.perform(get(randomEndpoint)
                    .with(user(authData.user().getEmail())));

            // Assert
            result.andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 404 when raffle does not exist")
        void shouldReturn404WhenRaffleDoesNotExist() throws Exception {
            // Arrange
            String nonExistentRaffleEndpoint = String.format("/v1/associations/%d/raffles/%d/tickets/random", 
                    authData.association().getId(), 999999L);

            // Act
            ResultActions result = mockMvc.perform(get(nonExistentRaffleEndpoint)
                    .param("quantity", "1")
                    .with(user(authData.user().getEmail())));

            // Assert
            result.andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Should return 401 when not authenticated")
        void shouldReturn401WhenNotAuthenticated() throws Exception {
            // Act
            ResultActions result = mockMvc.perform(get(randomEndpoint)
                    .param("quantity", "1"));

            // Assert
            result.andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("GET /v1/associations/{associationId}/raffles/{raffleId}/tickets - Edge Cases")
    class EdgeCasesTests {

        @Test
        @DisplayName("Should handle empty string filters gracefully")
        void shouldHandleEmptyStringFiltersGracefully() throws Exception {
            // Act
            ResultActions result = mockMvc.perform(get(searchEndpoint)
                    .param("ticketNumber", "")
                    .param("customerId", "")
                    .with(user(authData.user().getEmail())));

            // Assert
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content", hasSize(20))); // Default page size
        }

        @Test
        @DisplayName("Should return empty results when no tickets match criteria")
        void shouldReturnEmptyResultsWhenNoTicketsMatchCriteria() throws Exception {
            // Act
            ResultActions result = mockMvc.perform(get(searchEndpoint)
                    .param("ticketNumber", "NON-EXISTENT-TICKET")
                    .with(user(authData.user().getEmail())));

            // Assert
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content", hasSize(0)))
                    .andExpect(jsonPath("$.data.totalElements").value(0))
                    .andExpect(jsonPath("$.data.totalPages").value(0));
        }
    }
} 