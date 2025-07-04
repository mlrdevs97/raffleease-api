package com.raffleease.raffleease.Domains.Customers.Controller;

import com.raffleease.raffleease.Base.AbstractIntegrationTest;
import com.raffleease.raffleease.Domains.Customers.Model.Customer;
import com.raffleease.raffleease.Domains.Customers.Repository.CustomersRepository;
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

import static com.raffleease.raffleease.Domains.Raffles.Model.RaffleStatus.ACTIVE;
import static com.raffleease.raffleease.Domains.Tickets.Model.TicketStatus.AVAILABLE;
import static com.raffleease.raffleease.Domains.Tickets.Model.TicketStatus.SOLD;
import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("Customers Search Controller Integration Tests")
class CustomersSearchControllerIT extends AbstractIntegrationTest {

    @Autowired
    private AuthTestUtils authTestUtils;

    @Autowired
    private CustomersRepository customersRepository;

    @Autowired
    private RafflesRepository rafflesRepository;

    @Autowired
    private TicketsRepository ticketsRepository;

    private AuthTestData authData;
    private String searchEndpoint;
    private List<Customer> testCustomers;
    private List<Raffle> testRaffles;

    @BeforeEach
    void setUp() {
        authData = authTestUtils.createAuthenticatedUser();
        searchEndpoint = "/v1/associations/" + authData.association().getId() + "/customers";
        setupTestData();
    }

    private void setupTestData() {
        // Create test raffles
        testRaffles = createTestRaffles();
        
        // Create test customers
        testCustomers = createTestCustomers();
        
        // Create tickets to link customers to raffles (required for search)
        createTestTickets();
    }

    private List<Raffle> createTestRaffles() {
        List<Raffle> raffles = new ArrayList<>();
        
        for (int i = 1; i <= 3; i++) {
            RaffleStatistics statistics = TestDataBuilder.statistics()
                    .availableTickets(10L)
                    .soldTickets(0L)
                    .revenue(BigDecimal.ZERO)
                    .participants(0L)
                    .totalOrders(0L)
                    .build();

            Raffle raffle = TestDataBuilder.raffle()
                    .association(authData.association())
                    .status(ACTIVE)
                    .title("Test Raffle " + i)
                    .ticketPrice(BigDecimal.valueOf(10.00))
                    .totalTickets(10L)
                    .firstTicketNumber(1L)
                    .endDate(LocalDateTime.now().plusDays(30))
                    .statistics(statistics)
                    .build();
            raffles.add(rafflesRepository.save(raffle));
        }
        
        return raffles;
    }

    private List<Customer> createTestCustomers() {
        List<Customer> customers = new ArrayList<>();
        
        // Customer 1: John Doe
        customers.add(customersRepository.save(TestDataBuilder.customer()
                .fullName("John Doe")
                .email("john.doe@example.com")
                .phoneNumber("+1", "234567890")
                .build()));
        
        // Customer 2: Jane Smith
        customers.add(customersRepository.save(TestDataBuilder.customer()
                .fullName("Jane Smith")
                .email("jane.smith@example.com")
                .phoneNumber("+1", "987654321")
                .build()));
        
        // Customer 3: John Johnson
        customers.add(customersRepository.save(TestDataBuilder.customer()
                .fullName("John Johnson")
                .email("john.johnson@gmail.com")
                .phoneNumber("+1", "555123456")
                .build()));
        
        // Customer 4: Alice Brown
        customers.add(customersRepository.save(TestDataBuilder.customer()
                .fullName("Alice Brown")
                .email("alice.brown@yahoo.com")
                .phoneNumber("+1", "444987654")
                .build()));
        
        // Customer 5: Bob Wilson (no email)
        customers.add(customersRepository.save(TestDataBuilder.customer()
                .fullName("Bob Wilson")
                .noEmail()
                .phoneNumber("+1", "333456789")
                .build()));
        
        // Customer 6: Charlie Davis (no phone)
        customers.add(customersRepository.save(TestDataBuilder.customer()
                .fullName("Charlie Davis")
                .email("charlie.davis@hotmail.com")
                .noPhoneNumber()
                .build()));
        
        return customers;
    }

    private void createTestTickets() {
        for (int i = 0; i < 6; i++) { // Updated from 5 to 6 to include Charlie Davis
            Customer customer = testCustomers.get(i);
            Raffle raffle = testRaffles.get(i % testRaffles.size()); // Distribute across raffles
            
            // Create 1-2 tickets per customer
            for (int j = 1; j <= (i % 2) + 1; j++) {
                Ticket ticket = Ticket.builder()
                        .ticketNumber(String.valueOf((i * 10) + j))
                        .status(j == 1 ? SOLD : AVAILABLE) // First ticket sold, others available
                        .raffle(raffle)
                        .customer(customer)
                        .build();
                ticketsRepository.save(ticket);
            }
        }
    }

    @Nested
    @DisplayName("GET /v1/associations/{associationId}/customers - Basic Search")
    class BasicSearchTests {

        @Test
        @DisplayName("Should return all customers when no filters are applied")
        void shouldReturnAllCustomersWhenNoFiltersApplied() throws Exception {
            // Act
            ResultActions result = mockMvc.perform(get(searchEndpoint)
                    .with(user(authData.user().getEmail())));

            // Assert
            result.andExpect(status().isOk())
                    .andExpect(content().contentType("application/json"))
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Customers retrieved successfully"))
                    .andExpect(jsonPath("$.data.content", hasSize(6)))
                    .andExpect(jsonPath("$.data.totalElements").value(6))
                    .andExpect(jsonPath("$.data.totalPages").value(1))
                    .andExpect(jsonPath("$.data.first").value(true))
                    .andExpect(jsonPath("$.data.last").value(true));
        }

        @Test
        @DisplayName("Should return customers ordered by creation date descending")
        void shouldReturnCustomersOrderedByCreationDateDescending() throws Exception {
            // Act
            ResultActions result = mockMvc.perform(get(searchEndpoint)
                    .with(user(authData.user().getEmail())));

            // Assert - Verify ordering (most recent first) - now 6 customers returned
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content[0].fullName").value("Charlie Davis")) // Charlie Davis is now index 5, so most recent
                    .andExpect(jsonPath("$.data.content[1].fullName").value("Bob Wilson"))
                    .andExpect(jsonPath("$.data.content[2].fullName").value("Alice Brown"))
                    .andExpect(jsonPath("$.data.content[3].fullName").value("John Johnson"))
                    .andExpect(jsonPath("$.data.content[4].fullName").value("Jane Smith"))
                    .andExpect(jsonPath("$.data.content[5].fullName").value("John Doe"));
        }

        @Test
        @DisplayName("Should return empty result when association has no customers with tickets")
        void shouldReturnEmptyResultWhenAssociationHasNoCustomersWithTickets() throws Exception {
            // Arrange - Create new association with no customers
            AuthTestData otherAuthData = authTestUtils.createAuthenticatedUserWithCredentials(
                    "otheruser", "other@example.com", "password123");
            String otherEndpoint = "/v1/associations/" + otherAuthData.association().getId() + "/customers";

            // Act
            ResultActions result = mockMvc.perform(get(otherEndpoint)
                    .with(user(otherAuthData.user().getEmail())));

            // Assert
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.content", hasSize(0)))
                    .andExpect(jsonPath("$.data.totalElements").value(0));
        }
    }

    @Nested
    @DisplayName("GET /v1/associations/{associationId}/customers - Full Name Filter")
    class FullNameFilterTests {

        @Test
        @DisplayName("Should filter customers by full name (case insensitive)")
        void shouldFilterCustomersByFullNameCaseInsensitive() throws Exception {
            // Act
            ResultActions result = mockMvc.perform(get(searchEndpoint)
                    .param("fullName", "john")
                    .with(user(authData.user().getEmail())));

            // Assert
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content", hasSize(2)))
                    .andExpect(jsonPath("$.data.content[*].fullName", everyItem(containsStringIgnoringCase("john"))));
        }

        @Test
        @DisplayName("Should filter customers by partial full name")
        void shouldFilterCustomersByPartialFullName() throws Exception {
            // Act
            ResultActions result = mockMvc.perform(get(searchEndpoint)
                    .param("fullName", "doe")
                    .with(user(authData.user().getEmail())));

            // Assert
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content", hasSize(1)))
                    .andExpect(jsonPath("$.data.content[0].fullName").value("John Doe"));
        }

        @Test
        @DisplayName("Should return empty result when full name doesn't match any customer")
        void shouldReturnEmptyResultWhenFullNameDoesntMatch() throws Exception {
            // Act
            ResultActions result = mockMvc.perform(get(searchEndpoint)
                    .param("fullName", "nonexistent")
                    .with(user(authData.user().getEmail())));

            // Assert
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content", hasSize(0)))
                    .andExpect(jsonPath("$.data.totalElements").value(0));
        }

        @Test
        @DisplayName("Should handle full name filter with special characters")
        void shouldHandleFullNameFilterWithSpecialCharacters() throws Exception {
            // Arrange - Create customer with special characters
            Customer specialCustomer = customersRepository.save(TestDataBuilder.customer()
                    .fullName("José María O'Connor")
                    .email("jose@example.com")
                    .phoneNumber("+1", "999888777")
                    .build());
            
            // Create ticket to make customer searchable
            Ticket ticket = Ticket.builder()
                    .ticketNumber("999")
                    .status(SOLD)
                    .raffle(testRaffles.get(0))
                    .customer(specialCustomer)
                    .build();
            ticketsRepository.save(ticket);

            // Act
            ResultActions result = mockMvc.perform(get(searchEndpoint)
                    .param("fullName", "josé")
                    .with(user(authData.user().getEmail())));

            // Assert
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content", hasSize(1)))
                    .andExpect(jsonPath("$.data.content[0].fullName").value("José María O'Connor"));
        }
    }

    @Nested
    @DisplayName("GET /v1/associations/{associationId}/customers - Email Filter")
    class EmailFilterTests {

        @Test
        @DisplayName("Should filter customers by email (case insensitive)")
        void shouldFilterCustomersByEmailCaseInsensitive() throws Exception {
            // Act
            ResultActions result = mockMvc.perform(get(searchEndpoint)
                    .param("email", "JOHN")
                    .with(user(authData.user().getEmail())));

            // Assert
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content", hasSize(2)))
                    .andExpect(jsonPath("$.data.content[*].email", everyItem(containsStringIgnoringCase("john"))));
        }

        @Test
        @DisplayName("Should filter customers by email domain")
        void shouldFilterCustomersByEmailDomain() throws Exception {
            // Act
            ResultActions result = mockMvc.perform(get(searchEndpoint)
                    .param("email", "gmail")
                    .with(user(authData.user().getEmail())));

            // Assert
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content", hasSize(1)))
                    .andExpect(jsonPath("$.data.content[0].email").value("john.johnson@gmail.com"));
        }

        @Test
        @DisplayName("Should filter customers by partial email")
        void shouldFilterCustomersByPartialEmail() throws Exception {
            // Act
            ResultActions result = mockMvc.perform(get(searchEndpoint)
                    .param("email", "smith")
                    .with(user(authData.user().getEmail())));

            // Assert
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content", hasSize(1)))
                    .andExpect(jsonPath("$.data.content[0].email").value("jane.smith@example.com"));
        }

        @Test
        @DisplayName("Should return empty result when email doesn't match any customer")
        void shouldReturnEmptyResultWhenEmailDoesntMatch() throws Exception {
            // Act
            ResultActions result = mockMvc.perform(get(searchEndpoint)
                    .param("email", "nonexistent@domain.com")
                    .with(user(authData.user().getEmail())));

            // Assert
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content", hasSize(0)))
                    .andExpect(jsonPath("$.data.totalElements").value(0));
        }

        @Test
        @DisplayName("Should exclude customers with null email when filtering by email")
        void shouldExcludeCustomersWithNullEmailWhenFilteringByEmail() throws Exception {
            // Act
            ResultActions result = mockMvc.perform(get(searchEndpoint)
                    .param("email", "wilson")
                    .with(user(authData.user().getEmail())));

            // Assert - Bob Wilson has null email, should not be found
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content", hasSize(0)));
        }
    }

    @Nested
    @DisplayName("GET /v1/associations/{associationId}/customers - Phone Number Filter")
    class PhoneNumberFilterTests {

        @Test
        @DisplayName("Should filter customers by phone number")
        void shouldFilterCustomersByPhoneNumber() throws Exception {
            // Act
            ResultActions result = mockMvc.perform(get(searchEndpoint)
                    .param("phoneNumber", "+1234567890")
                    .with(user(authData.user().getEmail())));

            // Assert
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content", hasSize(1)))
                    .andExpect(jsonPath("$.data.content[0].phoneNumber.prefix").value("+1"))
                    .andExpect(jsonPath("$.data.content[0].phoneNumber.nationalNumber").value("234567890"));
        }

        @Test
        @DisplayName("Should filter customers by partial phone number")
        void shouldFilterCustomersByPartialPhoneNumber() throws Exception {
            // Act
            ResultActions result = mockMvc.perform(get(searchEndpoint)
                    .param("phoneNumber", "555")
                    .with(user(authData.user().getEmail())));

            // Assert
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content", hasSize(1)))
                    .andExpect(jsonPath("$.data.content[0].phoneNumber.prefix").value("+1"))
                    .andExpect(jsonPath("$.data.content[0].phoneNumber.nationalNumber").value("555123456"));
        }

        @Test
        @DisplayName("Should filter customers by phone number without country code")
        void shouldFilterCustomersByPhoneNumberWithoutCountryCode() throws Exception {
            // Act
            ResultActions result = mockMvc.perform(get(searchEndpoint)
                    .param("phoneNumber", "987654321")
                    .with(user(authData.user().getEmail())));

            // Assert
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content", hasSize(1)))
                    .andExpect(jsonPath("$.data.content[0].phoneNumber.prefix").value("+1"))
                    .andExpect(jsonPath("$.data.content[0].phoneNumber.nationalNumber").value("987654321"));
        }

        @Test
        @DisplayName("Should return empty result when phone number doesn't match any customer")
        void shouldReturnEmptyResultWhenPhoneNumberDoesntMatch() throws Exception {
            // Act
            ResultActions result = mockMvc.perform(get(searchEndpoint)
                    .param("phoneNumber", "9999999999")
                    .with(user(authData.user().getEmail())));

            // Assert
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content", hasSize(0)))
                    .andExpect(jsonPath("$.data.totalElements").value(0));
        }

        @Test
        @DisplayName("Should exclude customers with null phone number when filtering by phone")
        void shouldExcludeCustomersWithNullPhoneNumberWhenFilteringByPhone() throws Exception {
            // Act
            ResultActions result = mockMvc.perform(get(searchEndpoint)
                    .param("phoneNumber", "charlie")
                    .with(user(authData.user().getEmail())));

            // Assert - Charlie Davis has null phone, should not be found
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content", hasSize(0)));
        }
    }

    @Nested
    @DisplayName("GET /v1/associations/{associationId}/customers - Combined Filters")
    class CombinedFiltersTests {

        @Test
        @DisplayName("Should filter customers by full name and email")
        void shouldFilterCustomersByFullNameAndEmail() throws Exception {
            // Act
            ResultActions result = mockMvc.perform(get(searchEndpoint)
                    .param("fullName", "john")
                    .param("email", "doe")
                    .with(user(authData.user().getEmail())));

            // Assert
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content", hasSize(1)))
                    .andExpect(jsonPath("$.data.content[0].fullName").value("John Doe"))
                    .andExpect(jsonPath("$.data.content[0].email").value("john.doe@example.com"));
        }

        @Test
        @DisplayName("Should filter customers by full name and phone number")
        void shouldFilterCustomersByFullNameAndPhoneNumber() throws Exception {
            // Act
            ResultActions result = mockMvc.perform(get(searchEndpoint)
                    .param("fullName", "jane")
                    .param("phoneNumber", "+1987654321")
                    .with(user(authData.user().getEmail())));

            // Assert
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content", hasSize(1)))
                    .andExpect(jsonPath("$.data.content[0].fullName").value("Jane Smith"))
                    .andExpect(jsonPath("$.data.content[0].phoneNumber.prefix").value("+1"))
                    .andExpect(jsonPath("$.data.content[0].phoneNumber.nationalNumber").value("987654321"));
        }

        @Test
        @DisplayName("Should filter customers by email and phone number")
        void shouldFilterCustomersByEmailAndPhoneNumber() throws Exception {
            // Act
            ResultActions result = mockMvc.perform(get(searchEndpoint)
                    .param("email", "alice")
                    .param("phoneNumber", "444")
                    .with(user(authData.user().getEmail())));

            // Assert
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content", hasSize(1)))
                    .andExpect(jsonPath("$.data.content[0].fullName").value("Alice Brown"))
                    .andExpect(jsonPath("$.data.content[0].email").value("alice.brown@yahoo.com"));
        }

        @Test
        @DisplayName("Should filter customers by all three filters")
        void shouldFilterCustomersByAllThreeFilters() throws Exception {
            // Act
            ResultActions result = mockMvc.perform(get(searchEndpoint)
                    .param("fullName", "john")
                    .param("email", "johnson")
                    .param("phoneNumber", "555")
                    .with(user(authData.user().getEmail())));

            // Assert
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content", hasSize(1)))
                    .andExpect(jsonPath("$.data.content[0].fullName").value("John Johnson"))
                    .andExpect(jsonPath("$.data.content[0].email").value("john.johnson@gmail.com"))
                    .andExpect(jsonPath("$.data.content[0].phoneNumber.prefix").value("+1"))
                    .andExpect(jsonPath("$.data.content[0].phoneNumber.nationalNumber").value("555123456"));
        }

        @Test
        @DisplayName("Should return empty result when combined filters don't match any customer")
        void shouldReturnEmptyResultWhenCombinedFiltersDoNotMatch() throws Exception {
            // Act
            ResultActions result = mockMvc.perform(get(searchEndpoint)
                    .param("fullName", "john")
                    .param("email", "alice")
                    .with(user(authData.user().getEmail())));

            // Assert
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content", hasSize(0)))
                    .andExpect(jsonPath("$.data.totalElements").value(0));
        }
    }

    @Nested
    @DisplayName("GET /v1/associations/{associationId}/customers - Pagination")
    class PaginationTests {

        @Test
        @DisplayName("Should return first page with specified size")
        void shouldReturnFirstPageWithSpecifiedSize() throws Exception {
            // Act
            ResultActions result = mockMvc.perform(get(searchEndpoint)
                    .param("page", "0")
                    .param("size", "3")
                    .with(user(authData.user().getEmail())));

            // Assert
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content", hasSize(3)))
                    .andExpect(jsonPath("$.data.totalElements").value(6))
                    .andExpect(jsonPath("$.data.totalPages").value(2))
                    .andExpect(jsonPath("$.data.number").value(0))
                    .andExpect(jsonPath("$.data.size").value(3))
                    .andExpect(jsonPath("$.data.first").value(true))
                    .andExpect(jsonPath("$.data.last").value(false));
        }

        @Test
        @DisplayName("Should return second page with specified size")
        void shouldReturnSecondPageWithSpecifiedSize() throws Exception {
            // Act
            ResultActions result = mockMvc.perform(get(searchEndpoint)
                    .param("page", "1")
                    .param("size", "3")
                    .with(user(authData.user().getEmail())));

            // Assert
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content", hasSize(3)))
                    .andExpect(jsonPath("$.data.totalElements").value(6))
                    .andExpect(jsonPath("$.data.totalPages").value(2))
                    .andExpect(jsonPath("$.data.number").value(1))
                    .andExpect(jsonPath("$.data.size").value(3))
                    .andExpect(jsonPath("$.data.first").value(false))
                    .andExpect(jsonPath("$.data.last").value(true));
        }

        @Test
        @DisplayName("Should return empty page when page number exceeds available pages")
        void shouldReturnEmptyPageWhenPageNumberExceedsAvailablePages() throws Exception {
            // Act
            ResultActions result = mockMvc.perform(get(searchEndpoint)
                    .param("page", "10")
                    .param("size", "3")
                    .with(user(authData.user().getEmail())));

            // Assert
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content", hasSize(0)))
                    .andExpect(jsonPath("$.data.totalElements").value(6))
                    .andExpect(jsonPath("$.data.totalPages").value(2))
                    .andExpect(jsonPath("$.data.number").value(10));
        }

        @Test
        @DisplayName("Should handle pagination with filters")
        void shouldHandlePaginationWithFilters() throws Exception {
            // Act
            ResultActions result = mockMvc.perform(get(searchEndpoint)
                    .param("fullName", "john")
                    .param("page", "0")
                    .param("size", "1")
                    .with(user(authData.user().getEmail())));

            // Assert
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content", hasSize(1)))
                    .andExpect(jsonPath("$.data.totalElements").value(2))
                    .andExpect(jsonPath("$.data.totalPages").value(2))
                    .andExpect(jsonPath("$.data.first").value(true))
                    .andExpect(jsonPath("$.data.last").value(false));
        }
    }

    @Nested
    @DisplayName("GET /v1/associations/{associationId}/customers - Edge Cases and Validation")
    class EdgeCasesAndValidationTests {

        @Test
        @DisplayName("Should handle empty string filters")
        void shouldHandleEmptyStringFilters() throws Exception {
            // Act
            ResultActions result = mockMvc.perform(get(searchEndpoint)
                    .param("fullName", "")
                    .param("email", "")
                    .param("phoneNumber", "")
                    .with(user(authData.user().getEmail())));

            // Assert - Empty strings should be treated as no filter
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content", hasSize(6)))
                    .andExpect(jsonPath("$.data.totalElements").value(6));
        }

        @Test
        @DisplayName("Should handle whitespace-only filters")
        void shouldHandleWhitespaceOnlyFilters() throws Exception {
            // Act
            ResultActions result = mockMvc.perform(get(searchEndpoint)
                    .param("fullName", "   ")
                    .param("email", "\t")
                    .param("phoneNumber", "\n")
                    .with(user(authData.user().getEmail())));

            // Assert - Whitespace should be trimmed and treated as no filter
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content", hasSize(6)))
                    .andExpect(jsonPath("$.data.totalElements").value(6));
        }

        @Test
        @DisplayName("Should handle special characters in search filters")
        void shouldHandleSpecialCharactersInSearchFilters() throws Exception {
            // Act
            ResultActions result = mockMvc.perform(get(searchEndpoint)
                    .param("fullName", "!@#$%^&*()")
                    .param("email", "<script>alert('xss')</script>")
                    .param("phoneNumber", "'; DROP TABLE customers; --")
                    .with(user(authData.user().getEmail())));

            // Assert - Should not crash and return empty results
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content", hasSize(0)))
                    .andExpect(jsonPath("$.data.totalElements").value(0));
        }

        @Test
        @DisplayName("Should return 401 when user is not authenticated")
        void shouldReturn401WhenUserIsNotAuthenticated() throws Exception {
            // Act
            ResultActions result = mockMvc.perform(get(searchEndpoint));

            // Assert
            result.andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Should return 403 when user doesn't belong to association")
        void shouldReturn403WhenUserDoesntBelongToAssociation() throws Exception {
            // Arrange
            AuthTestData otherUserData = authTestUtils.createAuthenticatedUserWithCredentials(
                    "otheruser", "other@example.com", "password123");

            // Act
            ResultActions result = mockMvc.perform(get(searchEndpoint)
                    .with(user(otherUserData.user().getEmail())));

            // Assert
            result.andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Should return 403 when association doesn't exist")
        void shouldReturn403WhenAssociationDoesntExist() throws Exception {
            // Arrange
            String nonExistentAssociationEndpoint = "/v1/associations/99999/customers";

            // Act
            ResultActions result = mockMvc.perform(get(nonExistentAssociationEndpoint)
                    .with(user(authData.user().getEmail())));

            // Assert
            result.andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("GET /v1/associations/{associationId}/customers - Response Structure")
    class ResponseStructureTests {

        @Test
        @DisplayName("Should return correct customer DTO structure")
        void shouldReturnCorrectCustomerDTOStructure() throws Exception {
            // Act
            ResultActions result = mockMvc.perform(get(searchEndpoint)
                    .param("fullName", "john doe")
                    .with(user(authData.user().getEmail())));

            // Assert
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content[0].id").exists())
                    .andExpect(jsonPath("$.data.content[0].fullName").value("John Doe"))
                    .andExpect(jsonPath("$.data.content[0].email").value("john.doe@example.com"))
                    .andExpect(jsonPath("$.data.content[0].phoneNumber.prefix").value("+1"))
                    .andExpect(jsonPath("$.data.content[0].phoneNumber.nationalNumber").value("234567890"))
                    .andExpect(jsonPath("$.data.content[0].createdAt").exists())
                    .andExpect(jsonPath("$.data.content[0].updatedAt").exists());
        }

        @Test
        @DisplayName("Should handle customers with null email but valid phone number")
        void shouldHandleCustomersWithNullEmailButValidPhoneNumber() throws Exception {
            // Act
            ResultActions result = mockMvc.perform(get(searchEndpoint)
                    .param("fullName", "bob wilson")
                    .with(user(authData.user().getEmail())));

            // Assert
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content[0].fullName").value("Bob Wilson"))
                    .andExpect(jsonPath("$.data.content[0].email").isEmpty())
                    .andExpect(jsonPath("$.data.content[0].phoneNumber.prefix").value("+1"))
                    .andExpect(jsonPath("$.data.content[0].phoneNumber.nationalNumber").value("333456789"));
        }

        @Test
        @DisplayName("Should return correct pagination metadata")
        void shouldReturnCorrectPaginationMetadata() throws Exception {
            // Act
            ResultActions result = mockMvc.perform(get(searchEndpoint)
                    .param("page", "0")
                    .param("size", "2")
                    .with(user(authData.user().getEmail())));

            // Assert
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.pageable").exists())
                    .andExpect(jsonPath("$.data.totalElements").value(6))
                    .andExpect(jsonPath("$.data.totalPages").value(3))
                    .andExpect(jsonPath("$.data.size").value(2))
                    .andExpect(jsonPath("$.data.number").value(0))
                    .andExpect(jsonPath("$.data.numberOfElements").value(2))
                    .andExpect(jsonPath("$.data.first").value(true))
                    .andExpect(jsonPath("$.data.last").value(false))
                    .andExpect(jsonPath("$.data.empty").value(false));
        }
    }
} 