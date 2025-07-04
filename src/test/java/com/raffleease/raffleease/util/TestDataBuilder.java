package com.raffleease.raffleease.util;

import com.raffleease.raffleease.Domains.Associations.Model.Address;
import com.raffleease.raffleease.Domains.Associations.Model.Association;
import com.raffleease.raffleease.Domains.Associations.Model.AssociationMembership;
import com.raffleease.raffleease.Domains.Associations.Model.AssociationRole;
import com.raffleease.raffleease.Domains.Images.Model.Image;
import com.raffleease.raffleease.Domains.Images.Model.ImageStatus;
import com.raffleease.raffleease.Domains.Raffles.Model.Raffle;
import com.raffleease.raffleease.Domains.Raffles.Model.RaffleStatistics;
import com.raffleease.raffleease.Domains.Raffles.Model.RaffleStatus;
import com.raffleease.raffleease.Domains.Tickets.Model.Ticket;
import com.raffleease.raffleease.Domains.Tickets.Model.TicketStatus;
import com.raffleease.raffleease.Domains.Customers.Model.Customer;
import com.raffleease.raffleease.Domains.Customers.Model.CustomersPhoneNumber;
import com.raffleease.raffleease.Domains.Users.Model.User;
import com.raffleease.raffleease.Domains.Users.Model.UserPhoneNumber;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Test data builder following the Builder pattern for creating test entities.
 * Provides sensible defaults and allows customization of specific fields.
 */
public class TestDataBuilder {

    // Counter for generating unique image identifiers
    private static final AtomicInteger IMAGE_COUNTER = new AtomicInteger(1);

    public static UserBuilder user() {
        return new UserBuilder();
    }

    public static AssociationBuilder association() {
        return new AssociationBuilder();
    }

    public static AddressBuilder address() {
        return new AddressBuilder();
    }

    public static AssociationMembershipBuilder membership() {
        return new AssociationMembershipBuilder();
    }

    public static ImageBuilder image() {
        return new ImageBuilder();
    }

    public static RaffleBuilder raffle() {
        return new RaffleBuilder();
    }

    public static RaffleStatisticsBuilder statistics() {
        return new RaffleStatisticsBuilder();
    }

    public static TicketBuilder ticket() {
        return new TicketBuilder();
    }

    public static CustomerBuilder customer() {
        return new CustomerBuilder();
    }

    public static CustomersPhoneNumberBuilder customersPhoneNumber() {
        return new CustomersPhoneNumberBuilder();
    }

    public static class UserBuilder {
        private String firstName = "John";
        private String lastName = "Doe";
        private String userName = "johndoe";
        private String email = "john.doe@example.com";
        private UserPhoneNumber phoneNumber = UserPhoneNumber.builder()
                .prefix("+1")
                .nationalNumber("234567890")
                .build();
        private String password = "$2a$10$8K1p/3m4kNG6cZOsLZhxOuWyEZwEG4CqJ8Zz8J9hOqWyEZwEG4CqJ8";
        private boolean isEnabled = true;

        public UserBuilder firstName(String firstName) {
            this.firstName = firstName;
            return this;
        }

        public UserBuilder lastName(String lastName) {
            this.lastName = lastName;
            return this;
        }

        public UserBuilder userName(String userName) {
            this.userName = userName;
            return this;
        }

        public UserBuilder email(String email) {
            this.email = email;
            return this;
        }

        public UserBuilder phoneNumber(UserPhoneNumber phoneNumber) {
            this.phoneNumber = phoneNumber;
            return this;
        }

        public UserBuilder phoneNumber(String prefix, String nationalNumber) {
            this.phoneNumber = UserPhoneNumber.builder()
                    .prefix(prefix)
                    .nationalNumber(nationalNumber)
                    .build();
            return this;
        }

        public UserBuilder password(String password) {
            this.password = password;
            return this;
        }

        public UserBuilder enabled(boolean enabled) {
            this.isEnabled = enabled;
            return this;
        }

        public UserBuilder disabled() {
            this.isEnabled = false;
            return this;
        }

        public User build() {
            return User.builder()
                    .firstName(firstName)
                    .lastName(lastName)
                    .userName(userName)
                    .email(email)
                    .phoneNumber(phoneNumber)
                    .password(password)
                    .isEnabled(isEnabled)
                    .build();
        }
    }

    public static class AssociationBuilder {
        private String name = "Test Association";
        private String description = "A test association for integration testing";
        private String phoneNumber = "+1987654321";
        private String email = "contact@testassociation.com";
        private Address address;

        public AssociationBuilder name(String name) {
            this.name = name;
            return this;
        }

        public AssociationBuilder description(String description) {
            this.description = description;
            return this;
        }

        public AssociationBuilder phoneNumber(String phoneNumber) {
            this.phoneNumber = phoneNumber;
            return this;
        }

        public AssociationBuilder email(String email) {
            this.email = email;
            return this;
        }

        public AssociationBuilder address(Address address) {
            this.address = address;
            return this;
        }

        public Association build() {
            return Association.builder()
                    .name(name)
                    .description(description)
                    .phoneNumber(phoneNumber)
                    .email(email)
                    .address(address != null ? address : TestDataBuilder.address().build())
                    .memberships(new ArrayList<>())
                    .raffles(new ArrayList<>())
                    .build();
        }
    }

    public static class AddressBuilder {
        private String placeId = "ChIJOwg_06VPwokRYv534QaPC8g";
        private String formattedAddress = "New York, NY, USA";
        private Double latitude = 40.7128;
        private Double longitude = -74.0060;
        private String city = "New York";
        private String province = "NY";
        private String zipCode = "10001";

        public AddressBuilder placeId(String placeId) {
            this.placeId = placeId;
            return this;
        }

        public AddressBuilder formattedAddress(String formattedAddress) {
            this.formattedAddress = formattedAddress;
            return this;
        }

        public AddressBuilder latitude(Double latitude) {
            this.latitude = latitude;
            return this;
        }

        public AddressBuilder longitude(Double longitude) {
            this.longitude = longitude;
            return this;
        }

        public AddressBuilder city(String city) {
            this.city = city;
            return this;
        }

        public AddressBuilder province(String province) {
            this.province = province;
            return this;
        }

        public AddressBuilder zipCode(String zipCode) {
            this.zipCode = zipCode;
            return this;
        }

        public Address build() {
            return Address.builder()
                    .placeId(placeId)
                    .formattedAddress(formattedAddress)
                    .latitude(latitude)
                    .longitude(longitude)
                    .city(city)
                    .province(province)
                    .zipCode(zipCode)
                    .build();
        }
    }

    public static class AssociationMembershipBuilder {
        private User user;
        private Association association;
        private AssociationRole role = AssociationRole.ADMIN;

        public AssociationMembershipBuilder user(User user) {
            this.user = user;
            return this;
        }

        public AssociationMembershipBuilder association(Association association) {
            this.association = association;
            return this;
        }

        public AssociationMembershipBuilder role(AssociationRole role) {
            this.role = role;
            return this;
        }

        public AssociationMembershipBuilder adminRole() {
            this.role = AssociationRole.ADMIN;
            return this;
        }

        public AssociationMembershipBuilder memberRole() {
            this.role = AssociationRole.MEMBER;
            return this;
        }

        public AssociationMembership build() {
            return AssociationMembership.builder()
                    .user(user)
                    .association(association)
                    .role(role)
                    .build();
        }
    }

    public static class ImageBuilder {
        private String fileName = "test-image.jpg";
        private String filePath;
        private String contentType = "image/jpeg";
        private String url;
        private Integer imageOrder = 1;
        private Raffle raffle;
        private User user;
        private Association association;
        private ImageStatus status = ImageStatus.PENDING;

        public ImageBuilder() {
            // Generate unique values by default
            int imageNumber = IMAGE_COUNTER.getAndIncrement();
            this.filePath = "/test/path/" + this.fileName;
            this.url = "http://localhost/test/images/" + imageNumber;
        }

        public ImageBuilder fileName(String fileName) {
            this.fileName = fileName;
            return this;
        }

        public ImageBuilder filePath(String filePath) {
            this.filePath = filePath;
            return this;
        }

        public ImageBuilder contentType(String contentType) {
            this.contentType = contentType;
            return this;
        }

        public ImageBuilder url(String url) {
            this.url = url;
            return this;
        }

        public ImageBuilder imageOrder(Integer imageOrder) {
            this.imageOrder = imageOrder;
            return this;
        }

        public ImageBuilder raffle(Raffle raffle) {
            this.raffle = raffle;
            return this;
        }

        public ImageBuilder user(User user) {
            this.user = user;
            return this;
        }

        public ImageBuilder association(Association association) {
            this.association = association;
            return this;
        }

        public ImageBuilder status(ImageStatus status) {
            this.status = status;
            return this;
        }

        public ImageBuilder pendingImage() {
            this.raffle = null;
            this.status = ImageStatus.PENDING;
            return this;
        }

        public ImageBuilder activeImage() {
            this.status = ImageStatus.ACTIVE;
            return this;
        }

        public ImageBuilder markedForDeletion() {
            this.status = ImageStatus.MARKED_FOR_DELETION;
            return this;
        }

        public ImageBuilder jpegImage() {
            this.contentType = "image/jpeg";
            this.fileName = "test-image.jpg";
            return this;
        }

        public ImageBuilder pngImage() {
            this.contentType = "image/png";
            this.fileName = "test-image.png";
            return this;
        }

        public Image build() {
            return Image.builder()
                    .fileName(fileName)
                    .filePath(filePath)
                    .contentType(contentType)
                    .url(url)
                    .imageOrder(imageOrder)
                    .raffle(raffle)
                    .user(user)
                    .association(association)
                    .status(status)
                    .build();
        }
    }

    public static class RaffleBuilder {
        private String title = "Test Raffle";
        private String description = "A test raffle for integration testing";
        private RaffleStatus status = RaffleStatus.PENDING;
        private BigDecimal ticketPrice = BigDecimal.valueOf(10.00);
        private Long totalTickets = 50L;
        private Long firstTicketNumber = 1L;
        private LocalDateTime startDate = null;
        private LocalDateTime endDate = LocalDateTime.now().plusDays(7);
        private Association association;
        private List<Image> images = new ArrayList<>();
        private List<Ticket> tickets = new ArrayList<>();
        private RaffleStatistics statistics;

        public RaffleBuilder title(String title) {
            this.title = title;
            return this;
        }

        public RaffleBuilder description(String description) {
            this.description = description;
            return this;
        }

        public RaffleBuilder status(RaffleStatus status) {
            this.status = status;
            return this;
        }

        public RaffleBuilder ticketPrice(BigDecimal ticketPrice) {
            this.ticketPrice = ticketPrice;
            return this;
        }

        public RaffleBuilder totalTickets(Long totalTickets) {
            this.totalTickets = totalTickets;
            return this;
        }

        public RaffleBuilder firstTicketNumber(Long firstTicketNumber) {
            this.firstTicketNumber = firstTicketNumber;
            return this;
        }

        public RaffleBuilder startDate(LocalDateTime startDate) {
            this.startDate = startDate;
            return this;
        }

        public RaffleBuilder endDate(LocalDateTime endDate) {
            this.endDate = endDate;
            return this;
        }

        public RaffleBuilder association(Association association) {
            this.association = association;
            return this;
        }

        public RaffleBuilder images(List<Image> images) {
            this.images = images;
            return this;
        }

        public RaffleBuilder tickets(List<Ticket> tickets) {
            this.tickets = tickets;
            return this;
        }

        public RaffleBuilder statistics(RaffleStatistics statistics) {
            this.statistics = statistics;
            return this;
        }

        public Raffle build() {
            // Initialize statistics if not explicitly set
            if (statistics == null) {
                statistics = createDefaultStatistics(totalTickets);
            }
            
            Raffle raffle = Raffle.builder()
                    .title(title)
                    .description(description)
                    .status(status)
                    .ticketPrice(ticketPrice)
                    .totalTickets(totalTickets)
                    .firstTicketNumber(firstTicketNumber)
                    .startDate(startDate)
                    .endDate(endDate)
                    .association(association)
                    .images(images)
                    .tickets(tickets)
                    .statistics(statistics)
                    .build();
                    
            // Set the bidirectional relationship
            if (statistics != null) {
                statistics.setRaffle(raffle);
            }
            
            return raffle;
        }
        
        private RaffleStatistics createDefaultStatistics(Long totalTickets) {
            return RaffleStatistics.builder()
                    .availableTickets(totalTickets)
                    .soldTickets(0L)
                    .revenue(BigDecimal.ZERO)
                    .averageOrderValue(BigDecimal.ZERO)
                    .totalOrders(0L)
                    .completedOrders(0L)
                    .pendingOrders(0L)
                    .cancelledOrders(0L)
                    .unpaidOrders(0L)
                    .refundedOrders(0L)
                    .participants(0L)
                    .ticketsPerParticipant(BigDecimal.ZERO)
                    .firstSaleDate(null)
                    .lastSaleDate(null)
                    .dailySalesVelocity(BigDecimal.ZERO)
                    .build();
        }
    }

    public static class RaffleStatisticsBuilder {
        private Long availableTickets = 0L;
        private Long participants = 0L;
        private BigDecimal ticketsPerParticipant = BigDecimal.ZERO;
        private Long totalOrders = 0L;
        private Long pendingOrders = 0L;
        private Long completedOrders = 0L;
        private Long cancelledOrders = 0L;
        private Long unpaidOrders = 0L;
        private Long refundedOrders = 0L;
        private Long soldTickets = 0L;
        private BigDecimal revenue = BigDecimal.ZERO;
        private BigDecimal averageOrderValue = BigDecimal.ZERO;
        private BigDecimal dailySalesVelocity = BigDecimal.ZERO;
        private LocalDateTime firstSaleDate;
        private LocalDateTime lastSaleDate;

        public RaffleStatisticsBuilder availableTickets(Long availableTickets) {
            this.availableTickets = availableTickets;
            return this;
        }

        public RaffleStatisticsBuilder participants(Long participants) {
            this.participants = participants;
            return this;
        }

        public RaffleStatisticsBuilder ticketsPerParticipant(BigDecimal ticketsPerParticipant) {
            this.ticketsPerParticipant = ticketsPerParticipant;
            return this;
        }

        public RaffleStatisticsBuilder totalOrders(Long totalOrders) {
            this.totalOrders = totalOrders;
            return this;
        }

        public RaffleStatisticsBuilder pendingOrders(Long pendingOrders) {
            this.pendingOrders = pendingOrders;
            return this;
        }

        public RaffleStatisticsBuilder completedOrders(Long completedOrders) {
            this.completedOrders = completedOrders;
            return this;
        }

        public RaffleStatisticsBuilder cancelledOrders(Long cancelledOrders) {
            this.cancelledOrders = cancelledOrders;
            return this;
        }

        public RaffleStatisticsBuilder unpaidOrders(Long unpaidOrders) {
            this.unpaidOrders = unpaidOrders;
            return this;
        }

        public RaffleStatisticsBuilder refundedOrders(Long refundedOrders) {
            this.refundedOrders = refundedOrders;
            return this;
        }

        public RaffleStatisticsBuilder soldTickets(Long soldTickets) {
            this.soldTickets = soldTickets;
            return this;
        }

        public RaffleStatisticsBuilder revenue(BigDecimal revenue) {
            this.revenue = revenue;
            return this;
        }

        public RaffleStatisticsBuilder averageOrderValue(BigDecimal averageOrderValue) {
            this.averageOrderValue = averageOrderValue;
            return this;
        }

        public RaffleStatisticsBuilder dailySalesVelocity(BigDecimal dailySalesVelocity) {
            this.dailySalesVelocity = dailySalesVelocity;
            return this;
        }

        public RaffleStatisticsBuilder firstSaleDate(LocalDateTime firstSaleDate) {
            this.firstSaleDate = firstSaleDate;
            return this;
        }

        public RaffleStatisticsBuilder lastSaleDate(LocalDateTime lastSaleDate) {
            this.lastSaleDate = lastSaleDate;
            return this;
        }

        public RaffleStatistics build() {
            return RaffleStatistics.builder()
                    .availableTickets(availableTickets)
                    .participants(participants)
                    .ticketsPerParticipant(ticketsPerParticipant)
                    .totalOrders(totalOrders)
                    .pendingOrders(pendingOrders)
                    .completedOrders(completedOrders)
                    .cancelledOrders(cancelledOrders)
                    .unpaidOrders(unpaidOrders)
                    .refundedOrders(refundedOrders)
                    .soldTickets(soldTickets)
                    .revenue(revenue)
                    .averageOrderValue(averageOrderValue)
                    .dailySalesVelocity(dailySalesVelocity)
                    .firstSaleDate(firstSaleDate)
                    .lastSaleDate(lastSaleDate)
                    .build();
        }
    }

    public static class TicketBuilder {
        private String ticketNumber = "TICKET-001";
        private TicketStatus status = TicketStatus.AVAILABLE;
        private Raffle raffle;
        private Customer customer;

        public TicketBuilder ticketNumber(String ticketNumber) {
            this.ticketNumber = ticketNumber;
            return this;
        }

        public TicketBuilder status(TicketStatus status) {
            this.status = status;
            return this;
        }

        public TicketBuilder raffle(Raffle raffle) {
            this.raffle = raffle;
            return this;
        }

        public TicketBuilder customer(Customer customer) {
            this.customer = customer;
            return this;
        }

        public TicketBuilder available() {
            this.status = TicketStatus.AVAILABLE;
            this.customer = null;
            return this;
        }

        public TicketBuilder reserved() {
            this.status = TicketStatus.RESERVED;
            return this;
        }

        public TicketBuilder sold() {
            this.status = TicketStatus.SOLD;
            return this;
        }

        public Ticket build() {
            return Ticket.builder()
                    .ticketNumber(ticketNumber)
                    .status(status)
                    .raffle(raffle)
                    .customer(customer)
                    .build();
        }
    }

    public static class CustomerBuilder {
        private String fullName = "John Doe";
        private String email = "john.doe@example.com";
        private CustomersPhoneNumber phoneNumber = TestDataBuilder.customersPhoneNumber().build();

        public CustomerBuilder fullName(String fullName) {
            this.fullName = fullName;
            return this;
        }

        public CustomerBuilder email(String email) {
            this.email = email;
            return this;
        }

        public CustomerBuilder phoneNumber(CustomersPhoneNumber phoneNumber) {
            this.phoneNumber = phoneNumber;
            return this;
        }

        public CustomerBuilder phoneNumber(String prefix, String nationalNumber) {
            this.phoneNumber = CustomersPhoneNumber.builder()
                    .prefix(prefix)
                    .nationalNumber(nationalNumber)
                    .build();
            return this;
        }

        public CustomerBuilder noPhoneNumber() {
            this.phoneNumber = null;
            return this;
        }

        public CustomerBuilder noEmail() {
            this.email = null;
            return this;
        }

        public Customer build() {
            return Customer.builder()
                    .fullName(fullName)
                    .email(email)
                    .phoneNumber(phoneNumber)
                    .build();
        }
    }

    public static class CustomersPhoneNumberBuilder {
        private String prefix = "+1";
        private String nationalNumber = "234567890";

        public CustomersPhoneNumberBuilder prefix(String prefix) {
            this.prefix = prefix;
            return this;
        }

        public CustomersPhoneNumberBuilder nationalNumber(String nationalNumber) {
            this.nationalNumber = nationalNumber;
            return this;
        }

        public CustomersPhoneNumber build() {
            return CustomersPhoneNumber.builder()
                    .prefix(prefix)
                    .nationalNumber(nationalNumber)
                    .build();
        }
    }
} 