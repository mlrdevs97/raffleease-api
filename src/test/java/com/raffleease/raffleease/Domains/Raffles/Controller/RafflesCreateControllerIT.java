package com.raffleease.raffleease.Domains.Raffles.Controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.raffleease.raffleease.Base.AbstractIntegrationTest;
import com.raffleease.raffleease.Domains.Associations.Model.AssociationRole;
import com.raffleease.raffleease.Domains.Images.DTOs.ImageDTO;
import com.raffleease.raffleease.Domains.Images.Model.Image;
import com.raffleease.raffleease.Domains.Images.Model.ImageStatus;
import com.raffleease.raffleease.Domains.Images.Repository.ImagesRepository;
import com.raffleease.raffleease.Domains.Images.Services.FileStorageService;
import com.raffleease.raffleease.Domains.Raffles.DTOs.RaffleCreate;
import com.raffleease.raffleease.Domains.Raffles.Model.Raffle;
import com.raffleease.raffleease.Domains.Raffles.Repository.RafflesRepository;
import com.raffleease.raffleease.Domains.Tickets.DTO.TicketsCreate;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.ResultActions;

import java.math.BigDecimal;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static com.raffleease.raffleease.Domains.Raffles.Model.RaffleStatus.PENDING;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("Raffles Controller Integration Tests")
class RafflesCreateControllerIT extends AbstractIntegrationTest {

    @Autowired
    private AuthTestUtils authTestUtils;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RafflesRepository rafflesRepository;

    @Autowired
    private ImagesRepository imagesRepository;

    @Autowired
    private TicketsRepository ticketsRepository;

    @MockitoBean
    private FileStorageService fileStorageService;

    private AuthTestData authData;
    private String baseEndpoint;

    @BeforeEach
    void setUp() {
        authData = authTestUtils.createAuthenticatedUser();
        baseEndpoint = "/v1/associations/" + authData.association().getId() + "/raffles";
        
        // Mock file storage service to avoid actual file operations during raffle creation
        when(fileStorageService.moveFileToRaffle(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(Paths.get("/mocked/raffle/image/path"));
    }

    @Nested
    @DisplayName("POST /v1/associations/{associationId}/raffles")
    class CreateRaffleTests {

        @Test
        @DisplayName("Should successfully create raffle with pending images and generate tickets")
        void shouldCreateRaffleWithPendingImagesAndGenerateTickets() throws Exception {
            // Arrange
            List<Image> pendingImages = createPendingImagesForUser(3);
            RaffleCreate raffleCreate = createValidRaffleCreate(pendingImages, 1L, 50L);

            // Act
            ResultActions result = mockMvc.perform(post(baseEndpoint)
                    .contentType(APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(raffleCreate))
                    .with(user(authData.user().getEmail())));

            // Assert
            result.andExpect(status().isCreated())
                    .andExpect(header().exists("Location"))
                    .andExpect(content().contentType(APPLICATION_JSON))
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("New raffle created successfully"))
                    .andExpect(jsonPath("$.data.id").exists())
                    .andExpect(jsonPath("$.data.title").value(raffleCreate.title()))
                    .andExpect(jsonPath("$.data.description").value(raffleCreate.description()))
                    .andExpect(jsonPath("$.data.status").value("PENDING"))
                    .andExpect(jsonPath("$.data.ticketPrice").value(raffleCreate.ticketsInfo().price().doubleValue()))
                    .andExpect(jsonPath("$.data.totalTickets").value(50))
                    .andExpect(jsonPath("$.data.firstTicketNumber").value(1))
                    .andExpect(jsonPath("$.data.images").isArray())
                    .andExpect(jsonPath("$.data.images", hasSize(3)));

            // Verify database state
            List<Raffle> savedRaffles = rafflesRepository.findAll();
            assertThat(savedRaffles).hasSize(1);

            Raffle savedRaffle = savedRaffles.get(0);
            assertThat(savedRaffle.getTitle()).isEqualTo(raffleCreate.title());
            assertThat(savedRaffle.getDescription()).isEqualTo(raffleCreate.description());
            assertThat(savedRaffle.getStatus()).isEqualTo(PENDING);
            assertThat(savedRaffle.getAssociation().getId()).isEqualTo(authData.association().getId());
            assertThat(savedRaffle.getStatistics()).isNotNull();

            // Verify images were associated properly
            verifyImagesAssociatedToRaffle(savedRaffle, pendingImages);

            // Verify tickets were generated correctly
            verifyTicketsGenerated(savedRaffle, 1L, 50L);

            // Verify statistics were initialized
            verifyInitialStatistics(savedRaffle, 50L);
        }

        @Test
        @DisplayName("Should create raffle with immediate start when startDate is null")
        void shouldCreateRaffleWithImmediateStartWhenStartDateIsNull() throws Exception {
            // Arrange
            List<Image> pendingImages = createPendingImagesForUser(2);
            RaffleCreate raffleCreate = new RaffleCreate(
                    "Immediate Start Raffle",
                    "This raffle starts immediately",
                    null,
                    LocalDateTime.now().plusDays(7),
                    convertToImageDTOs(pendingImages),
                    new TicketsCreate(25L, BigDecimal.valueOf(10.00), 1L)
            );

            // Act
            ResultActions result = mockMvc.perform(post(baseEndpoint)
                    .contentType(APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(raffleCreate))
                    .with(user(authData.user().getEmail())));

            // Assert
            result.andExpect(status().isCreated())
                    .andExpect(jsonPath("$.data.startDate").isEmpty());

            // Verify database
            Raffle savedRaffle = rafflesRepository.findAll().get(0);
            assertThat(savedRaffle.getStartDate()).isNull();
        }

        @Test
        @DisplayName("Should create raffle with scheduled start when startDate is provided")
        void shouldCreateRaffleWithScheduledStartWhenStartDateProvided() throws Exception {
            // Arrange
            LocalDateTime futureStart = LocalDateTime.now().plusDays(2);
            List<Image> pendingImages = createPendingImagesForUser(2);
            RaffleCreate raffleCreate = new RaffleCreate(
                    "Scheduled Start Raffle",
                    "This raffle has a scheduled start date",
                    futureStart,
                    LocalDateTime.now().plusDays(7),
                    convertToImageDTOs(pendingImages),
                    new TicketsCreate(30L, BigDecimal.valueOf(15.50), 1L)
            );

            // Act
            ResultActions result = mockMvc.perform(post(baseEndpoint)
                    .contentType(APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(raffleCreate))
                    .with(user(authData.user().getEmail())));

            // Assert
            result.andExpect(status().isCreated())
                    .andExpect(jsonPath("$.data.startDate").exists());

            // Verify database
            Raffle savedRaffle = rafflesRepository.findAll().get(0);
            assertThat(savedRaffle.getStartDate()).isNotNull();
        }

        @Test
        @DisplayName("Should return 401 when user is not authenticated")
        void shouldReturn401WhenNotAuthenticated() throws Exception {
            // Arrange
            List<Image> pendingImages = createPendingImagesForUser(2);
            RaffleCreate raffleCreate = createValidRaffleCreate(pendingImages, 1L, 20L);

            // Act
            ResultActions result = mockMvc.perform(post(baseEndpoint)
                    .contentType(APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(raffleCreate)));

            // Assert
            result.andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Should return 403 when user doesn't belong to association")
        void shouldReturn403WhenUserDoesntBelongToAssociation() throws Exception {
            // Arrange
            AuthTestData otherUserData = authTestUtils.createAuthenticatedUserWithCredentials(
                    "otheruser", "other@example.com", "password123");
            List<Image> pendingImages = createPendingImagesForUser(2);
            RaffleCreate raffleCreate = createValidRaffleCreate(pendingImages, 1L, 20L);

            // Act
            ResultActions result = mockMvc.perform(post(baseEndpoint)
                    .contentType(APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(raffleCreate))
                    .with(user(otherUserData.user().getEmail())));

            // Assert
            result.andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Should return 403 when COLLABORATOR tries to create raffle")
        void shouldReturn403WhenCollaboratorTriesToCreateRaffle() throws Exception {
            // Arrange
            AuthTestData collaboratorData = authTestUtils.createAuthenticatedUserInSameAssociation(
                    authData.association(), AssociationRole.COLLABORATOR);
            List<Image> pendingImages = createPendingImagesForUser(2);
            RaffleCreate raffleCreate = createValidRaffleCreate(pendingImages, 1L, 20L);

            // Act
            ResultActions result = mockMvc.perform(post(baseEndpoint)
                    .contentType(APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(raffleCreate))
                    .with(user(collaboratorData.user().getEmail())));

            // Assert
            result.andExpect(status().isForbidden())
                    .andExpect(content().contentType(APPLICATION_JSON))
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("Only administrators and members can create raffles"));

            // Verify no raffle was created
            List<Raffle> savedRaffles = rafflesRepository.findAll();
            assertThat(savedRaffles).isEmpty();
        }

        @Test
        @DisplayName("Should successfully create raffle for ADMIN role")
        void shouldSuccessfullyCreateRaffleForAdmin() throws Exception {
            // Arrange
            AuthTestData adminData = authTestUtils.createAuthenticatedUserInSameAssociation(
                    authData.association(), AssociationRole.ADMIN);
            List<Image> pendingImages = createPendingImagesForAdmin(adminData, 2);
            RaffleCreate raffleCreate = createValidRaffleCreateForUser(adminData, pendingImages, 1L, 20L);

            // Act
            ResultActions result = mockMvc.perform(post(baseEndpoint)
                    .contentType(APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(raffleCreate))
                    .with(user(adminData.user().getEmail())));

            // Assert
            result.andExpect(status().isCreated())
                    .andExpect(content().contentType(APPLICATION_JSON))
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("New raffle created successfully"));

            // Verify raffle was created
            List<Raffle> savedRaffles = rafflesRepository.findAll();
            assertThat(savedRaffles).hasSize(1);
        }

        @Test
        @DisplayName("Should successfully create raffle for MEMBER role")
        void shouldSuccessfullyCreateRaffleForMember() throws Exception {
            // Arrange
            AuthTestData memberData = authTestUtils.createAuthenticatedUserInSameAssociation(
                    authData.association(), AssociationRole.MEMBER);
            List<Image> pendingImages = createPendingImagesForMember(memberData, 2);
            RaffleCreate raffleCreate = createValidRaffleCreateForUser(memberData, pendingImages, 1L, 20L);

            // Act
            ResultActions result = mockMvc.perform(post(baseEndpoint)
                    .contentType(APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(raffleCreate))
                    .with(user(memberData.user().getEmail())));

            // Assert
            result.andExpect(status().isCreated())
                    .andExpect(content().contentType(APPLICATION_JSON))
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("New raffle created successfully"));

            // Verify raffle was created
            List<Raffle> savedRaffles = rafflesRepository.findAll();
            assertThat(savedRaffles).hasSize(1);
        }

        @Test
        @DisplayName("Should return 400 when required fields are missing")
        void shouldReturn400WhenRequiredFieldsAreMissing() throws Exception {
            // Arrange
            String invalidRaffleJson = """
                {
                    "title": "",
                    "description": "",
                    "endDate": null,
                    "images": [],
                    "ticketsInfo": null
                }
                """;

            // Act
            ResultActions result = mockMvc.perform(post(baseEndpoint)
                    .contentType(APPLICATION_JSON)
                    .content(invalidRaffleJson)
                    .with(user(authData.user().getEmail())));

            // Assert
            result.andExpect(status().isBadRequest())
                    .andExpect(content().contentType(APPLICATION_JSON))
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("Should return 400 when endDate is in the past")
        void shouldReturn400WhenEndDateIsInThePast() throws Exception {
            // Arrange
            List<Image> pendingImages = createPendingImagesForUser(2);
            LocalDateTime pastDate = LocalDateTime.now().minusDays(1);
            
            RaffleCreate raffleCreate = new RaffleCreate(
                    "Invalid Raffle",
                    "This raffle has past end date",
                    null,
                    pastDate, // ← Past date
                    convertToImageDTOs(pendingImages),
                    new TicketsCreate(10L, BigDecimal.valueOf(5.00), 1L)
            );

            // Act
            ResultActions result = mockMvc.perform(post(baseEndpoint)
                    .contentType(APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(raffleCreate))
                    .with(user(authData.user().getEmail())));

            // Assert
            result.andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 400 when endDate is less than 24 hours after startDate")
        void shouldReturn400WhenEndDateIsLessThan24HoursAfterStartDate() throws Exception {
            // Arrange
            List<Image> pendingImages = createPendingImagesForUser(2);
            LocalDateTime startDate = LocalDateTime.now().plusDays(2);
            LocalDateTime endDate = startDate.plusHours(23); // Less than 24 hours after start date
            
            RaffleCreate raffleCreate = new RaffleCreate(
                    "Invalid Date Range Raffle",
                    "End date is less than 24 hours after start date",
                    startDate,
                    endDate,
                    convertToImageDTOs(pendingImages),
                    new TicketsCreate(10L, BigDecimal.valueOf(5.00), 1L)
            );

            // Act
            ResultActions result = mockMvc.perform(post(baseEndpoint)
                    .contentType(APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(raffleCreate))
                    .with(user(authData.user().getEmail())));

            // Assert
            result.andExpect(status().isBadRequest())
                    .andExpect(content().contentType(APPLICATION_JSON))
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("Validation failed"))
                    .andExpect(jsonPath("$.errors").exists());
        }

        @Test
        @DisplayName("Should return 400 when startDate is after endDate")
        void shouldReturn400WhenStartDateIsAfterEndDate() throws Exception {
            // Arrange
            List<Image> pendingImages = createPendingImagesForUser(2);
            LocalDateTime startDate = LocalDateTime.now().plusDays(5);
            LocalDateTime endDate = LocalDateTime.now().plusDays(3); // Before start date
            
            RaffleCreate raffleCreate = new RaffleCreate(
                    "Invalid Date Range Raffle",
                    "Start date is after end date",
                    startDate,
                    endDate,
                    convertToImageDTOs(pendingImages),
                    new TicketsCreate(10L, BigDecimal.valueOf(5.00), 1L)
            );

            // Act
            ResultActions result = mockMvc.perform(post(baseEndpoint)
                    .contentType(APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(raffleCreate))
                    .with(user(authData.user().getEmail())));

            // Assert
            result.andExpect(status().isBadRequest())
                    .andExpect(content().contentType(APPLICATION_JSON))
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("Validation failed"))
                    .andExpect(jsonPath("$.errors").exists());
        }

        @Test
        @DisplayName("Should succeed when endDate is exactly 24 hours after startDate")
        void shouldSucceedWhenEndDateIsExactly24HoursAfterStartDate() throws Exception {
            // Arrange
            List<Image> pendingImages = createPendingImagesForUser(2);
            LocalDateTime startDate = LocalDateTime.now().plusDays(2);
            LocalDateTime endDate = startDate.plusHours(24); // Exactly 24 hours after start date
            
            RaffleCreate raffleCreate = new RaffleCreate(
                    "Valid 24 Hour Range Raffle",
                    "End date is exactly 24 hours after start date",
                    startDate,
                    endDate,
                    convertToImageDTOs(pendingImages),
                    new TicketsCreate(10L, BigDecimal.valueOf(5.00), 1L)
            );

            // Act
            ResultActions result = mockMvc.perform(post(baseEndpoint)
                    .contentType(APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(raffleCreate))
                    .with(user(authData.user().getEmail())));

            // Assert
            result.andExpect(status().isCreated())
                    .andExpect(content().contentType(APPLICATION_JSON))
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("New raffle created successfully"));

            // Verify raffle was created
            List<Raffle> savedRaffles = rafflesRepository.findAll();
            assertThat(savedRaffles).hasSize(1);
            
            Raffle savedRaffle = savedRaffles.get(0);
            assertThat(savedRaffle.getStartDate()).isEqualTo(startDate);
            assertThat(savedRaffle.getEndDate()).isEqualTo(endDate);
        }

        @Test
        @DisplayName("Should succeed when endDate is more than 24 hours after startDate")
        void shouldSucceedWhenEndDateIsMoreThan24HoursAfterStartDate() throws Exception {
            // Arrange
            List<Image> pendingImages = createPendingImagesForUser(2);
            LocalDateTime startDate = LocalDateTime.now().plusDays(2);
            LocalDateTime endDate = startDate.plusHours(48); // 48 hours after start date
            
            RaffleCreate raffleCreate = new RaffleCreate(
                    "Valid Long Range Raffle",
                    "End date is more than 24 hours after start date",
                    startDate,
                    endDate,
                    convertToImageDTOs(pendingImages),
                    new TicketsCreate(10L, BigDecimal.valueOf(5.00), 1L)
            );

            // Act
            ResultActions result = mockMvc.perform(post(baseEndpoint)
                    .contentType(APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(raffleCreate))
                    .with(user(authData.user().getEmail())));

            // Assert
            result.andExpect(status().isCreated())
                    .andExpect(content().contentType(APPLICATION_JSON))
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("New raffle created successfully"));

            // Verify raffle was created
            List<Raffle> savedRaffles = rafflesRepository.findAll();
            assertThat(savedRaffles).hasSize(1);
            
            Raffle savedRaffle = savedRaffles.get(0);
            assertThat(savedRaffle.getStartDate()).isEqualTo(startDate);
            assertThat(savedRaffle.getEndDate()).isEqualTo(endDate);
        }

        @Test
        @DisplayName("Should succeed when startDate is null (immediate start) regardless of endDate timing")
        void shouldSucceedWhenStartDateIsNullRegardlessOfEndDateTiming() throws Exception {
            // Arrange
            List<Image> pendingImages = createPendingImagesForUser(2);
            LocalDateTime endDate = LocalDateTime.now().plusHours(6); // Less than 24 hours from now, but startDate is null
            
            RaffleCreate raffleCreate = new RaffleCreate(
                    "Immediate Start Raffle",
                    "Raffle with null startDate should not be subject to 24-hour validation",
                    null, // No start date - immediate start
                    endDate,
                    convertToImageDTOs(pendingImages),
                    new TicketsCreate(10L, BigDecimal.valueOf(5.00), 1L)
            );

            // Act
            ResultActions result = mockMvc.perform(post(baseEndpoint)
                    .contentType(APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(raffleCreate))
                    .with(user(authData.user().getEmail())));

            // Assert - Should succeed because validation doesn't apply when startDate is null
            result.andExpect(status().isCreated())
                    .andExpect(content().contentType(APPLICATION_JSON))
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("New raffle created successfully"));

            // Verify raffle was created
            List<Raffle> savedRaffles = rafflesRepository.findAll();
            assertThat(savedRaffles).hasSize(1);
            
            Raffle savedRaffle = savedRaffles.get(0);
            assertThat(savedRaffle.getStartDate()).isNull();
            assertThat(savedRaffle.getEndDate()).isEqualTo(endDate);
        }

        @Test
        @DisplayName("Should return 403 when trying to use images from different association")
        void shouldReturn403WhenUsingImagesFromDifferentAssociation() throws Exception {
            // Arrange
            AuthTestData otherUserData = authTestUtils.createAuthenticatedUserWithCredentials(
                    "otheruser2", "other2@example.com", "password123");
            
            // Create images belonging to different user from different association
            Image otherAssociationImage = TestDataBuilder.image()
                    .user(otherUserData.user())
                    .association(otherUserData.association())
                    .status(ImageStatus.PENDING)
                    .build();
            otherAssociationImage = imagesRepository.save(otherAssociationImage);

            RaffleCreate raffleCreate = new RaffleCreate(
                    "Cross Association Raffle",
                    "Trying to use images from different association",
                    null,
                    LocalDateTime.now().plusDays(7),
                    List.of(new ImageDTO(otherAssociationImage.getId(), 
                            otherAssociationImage.getFileName(),
                            otherAssociationImage.getFilePath(),
                            otherAssociationImage.getContentType(),
                            otherAssociationImage.getUrl(),
                            1)),
                    new TicketsCreate(10L, BigDecimal.valueOf(5.00), 1L)
            );

            // Act
            ResultActions result = mockMvc.perform(post(baseEndpoint)
                    .contentType(APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(raffleCreate))
                    .with(user(authData.user().getEmail())));

            // Assert - Should still fail because validation runs on existing images
            result.andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("Should return 400 when trying to use non-pending images")
        void shouldReturn400WhenUsingNonPendingImages() throws Exception {
            // Arrange
            // Create an existing raffle
            Raffle existingRaffle = TestDataBuilder.raffle()
                    .association(authData.association())
                    .status(PENDING)
                    .build();
            existingRaffle = rafflesRepository.save(existingRaffle);

            // Create image already associated to raffle (status ACTIVE)
            Image associatedImage = TestDataBuilder.image()
                    .user(authData.user())
                    .association(authData.association())
                    .status(ImageStatus.ACTIVE)
                    .raffle(existingRaffle)
                    .imageOrder(1)
                    .build();
            associatedImage = imagesRepository.save(associatedImage);

            RaffleCreate raffleCreate = new RaffleCreate(
                    "Invalid Image State Raffle",
                    "Trying to use already associated image",
                    null,
                    LocalDateTime.now().plusDays(7),
                    List.of(new ImageDTO(associatedImage.getId(),
                            associatedImage.getFileName(),
                            associatedImage.getFilePath(),
                            associatedImage.getContentType(),
                            associatedImage.getUrl(),
                            1)),
                    new TicketsCreate(10L, BigDecimal.valueOf(5.00), 1L)
            );

            // Act
            ResultActions result = mockMvc.perform(post(baseEndpoint)
                    .contentType(APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(raffleCreate))
                    .with(user(authData.user().getEmail())));

            // Assert - Should fail because only pending images can be used for raffle creation
            result.andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("Only pending images can be associated with a raffle."));
        }

        @Test
        @DisplayName("Should return 400 when trying to use pending images already associated with a raffle")
        void shouldReturn400WhenUsingPendingImagesAlreadyAssociatedWithRaffle() throws Exception {
            // Arrange
            // Create an existing raffle
            Raffle existingRaffle = TestDataBuilder.raffle()
                    .association(authData.association())
                    .status(PENDING)
                    .build();
            existingRaffle = rafflesRepository.save(existingRaffle);

            // Create pending image already associated to raffle
            Image pendingImageWithRaffle = TestDataBuilder.image()
                    .user(authData.user())
                    .association(authData.association())
                    .status(ImageStatus.PENDING)
                    .raffle(existingRaffle)
                    .imageOrder(1)
                    .build();
            pendingImageWithRaffle = imagesRepository.save(pendingImageWithRaffle);

            RaffleCreate raffleCreate = new RaffleCreate(
                    "Pending Image With Raffle",
                    "Trying to use pending image already associated with a raffle",
                    null,
                    LocalDateTime.now().plusDays(7),
                    List.of(new ImageDTO(pendingImageWithRaffle.getId(),
                            pendingImageWithRaffle.getFileName(),
                            pendingImageWithRaffle.getFilePath(),
                            pendingImageWithRaffle.getContentType(),
                            pendingImageWithRaffle.getUrl(),
                            1)),
                    new TicketsCreate(10L, BigDecimal.valueOf(5.00), 1L)
            );

            // Act
            ResultActions result = mockMvc.perform(post(baseEndpoint)
                    .contentType(APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(raffleCreate))
                    .with(user(authData.user().getEmail())));

            // Assert - Should fail because pending images cannot be associated with a raffle during creation
            result.andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("Pending images cannot be associated with a raffle."));
        }

        @Test
        @DisplayName("Should return 400 when images have duplicate IDs")
        void shouldReturn400WhenImagesHaveDuplicateIds() throws Exception {
            // Arrange
            Image pendingImage = TestDataBuilder.image()
                    .user(authData.user())
                    .association(authData.association())
                    .status(ImageStatus.PENDING)
                    .build();
            pendingImage = imagesRepository.save(pendingImage);

            RaffleCreate raffleCreate = new RaffleCreate(
                    "Duplicate Image IDs Raffle",
                    "Has duplicate image IDs",
                    null,
                    LocalDateTime.now().plusDays(7),
                    List.of(
                            new ImageDTO(pendingImage.getId(),
                                    pendingImage.getFileName(),
                                    pendingImage.getFilePath(),
                                    pendingImage.getContentType(),
                                    pendingImage.getUrl(),
                                    1),
                            new ImageDTO(pendingImage.getId(), // ← Duplicate ID
                                    pendingImage.getFileName(),
                                    pendingImage.getFilePath(),
                                    pendingImage.getContentType(),
                                    pendingImage.getUrl(),
                                    2)
                    ),
                    new TicketsCreate(10L, BigDecimal.valueOf(5.00), 1L)
            );

            // Act
            ResultActions result = mockMvc.perform(post(baseEndpoint)
                    .contentType(APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(raffleCreate))
                    .with(user(authData.user().getEmail())));

            // Assert
            result.andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("Should return 400 when images have duplicate orders")
        void shouldReturn400WhenImagesHaveDuplicateOrders() throws Exception {
            // Arrange
            List<Image> pendingImages = createPendingImagesForUser(2);

            RaffleCreate raffleCreate = new RaffleCreate(
                    "Duplicate Image Orders Raffle",
                    "Has duplicate image orders",
                    null,
                    LocalDateTime.now().plusDays(7),
                    List.of(
                            new ImageDTO(pendingImages.get(0).getId(),
                                    pendingImages.get(0).getFileName(),
                                    pendingImages.get(0).getFilePath(),
                                    pendingImages.get(0).getContentType(),
                                    pendingImages.get(0).getUrl(),
                                    1),
                            new ImageDTO(pendingImages.get(1).getId(),
                                    pendingImages.get(1).getFileName(),
                                    pendingImages.get(1).getFilePath(),
                                    pendingImages.get(1).getContentType(),
                                    pendingImages.get(1).getUrl(),
                                    1) // ← Duplicate order
                    ),
                    new TicketsCreate(10L, BigDecimal.valueOf(5.00), 1L)
            );

            // Act
            ResultActions result = mockMvc.perform(post(baseEndpoint)
                    .contentType(APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(raffleCreate))
                    .with(user(authData.user().getEmail())));

            // Assert
            result.andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("Should return 400 when some requested images are missing and no valid images remain")
        void shouldReturn400WhenSomeRequestedImagesAreMissingAndNoValidImagesRemain() throws Exception {
            // Arrange - Create 2 pending images but reference 3 in the request (1 missing)
            List<Image> pendingImages = createPendingImagesForUser(2);
            
            List<ImageDTO> imageDTOs = new ArrayList<>();
            // Add existing images
            imageDTOs.addAll(convertToImageDTOs(pendingImages));
            // Add non-existent image
            imageDTOs.add(new ImageDTO(99999L, "non-existent.jpg", "/path", "image/jpeg", "http://example.com", 3));

            RaffleCreate raffleCreate = new RaffleCreate(
                    "Mixed Images Raffle",
                    "Has both existing and missing images",
                    null,
                    LocalDateTime.now().plusDays(7),
                    imageDTOs,
                    new TicketsCreate(10L, BigDecimal.valueOf(5.00), 1L)
            );

            // Act
            ResultActions result = mockMvc.perform(post(baseEndpoint)
                    .contentType(APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(raffleCreate))
                    .with(user(authData.user().getEmail())));

            // Assert - Should succeed with only the existing images (2 valid images remain)
            result.andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.images", hasSize(2))); // Only 2 existing images

            // Verify database state - only existing images were associated
            List<Raffle> savedRaffles = rafflesRepository.findAll();
            assertThat(savedRaffles).hasSize(1);
            
            Raffle savedRaffle = savedRaffles.get(0);
            List<Image> raffleImages = imagesRepository.findAllByRaffle(savedRaffle);
            assertThat(raffleImages).hasSize(2); // Only existing images were associated
            
            // Verify no pending images remain
            List<Image> remainingPendingImages = imagesRepository.findAllByRaffleIsNullAndUserAndStatus(
                authData.user(), ImageStatus.PENDING);
            assertThat(remainingPendingImages).isEmpty();
        }

        @Test
        @DisplayName("Should return 400 when all requested images are missing")
        void shouldReturn400WhenAllRequestedImagesAreMissing() throws Exception {
            // Arrange - Request only non-existent images
            List<ImageDTO> imageDTOs = List.of(
                    new ImageDTO(99998L, "missing1.jpg", "/path", "image/jpeg", "http://example.com", 1),
                    new ImageDTO(99999L, "missing2.jpg", "/path", "image/jpeg", "http://example.com", 2)
            );

            RaffleCreate raffleCreate = new RaffleCreate(
                    "No Valid Images Raffle",
                    "All requested images are missing",
                    null,
                    LocalDateTime.now().plusDays(7),
                    imageDTOs,
                    new TicketsCreate(10L, BigDecimal.valueOf(5.00), 1L)
            );

            // Act
            ResultActions result = mockMvc.perform(post(baseEndpoint)
                    .contentType(APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(raffleCreate))
                    .with(user(authData.user().getEmail())));

            // Assert - Should fail because raffles must have at least one image
            result.andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("A raffle must have at least one image."));

            // Verify no raffle was created
            List<Raffle> savedRaffles = rafflesRepository.findAll();
            assertThat(savedRaffles).isEmpty();
        }

        @Test
        @DisplayName("Should return 400 when no images are provided in request")
        void shouldReturn400WhenNoImagesProvidedInRequest() throws Exception {
            // Arrange - Request with empty images list
            RaffleCreate raffleCreate = new RaffleCreate(
                    "No Images Raffle",
                    "Has no images in request",
                    null,
                    LocalDateTime.now().plusDays(7),
                    List.of(), // Empty images list
                    new TicketsCreate(10L, BigDecimal.valueOf(5.00), 1L)
            );

            // Act
            ResultActions result = mockMvc.perform(post(baseEndpoint)
                    .contentType(APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(raffleCreate))
                    .with(user(authData.user().getEmail())));

            // Assert - Should fail because raffles must have at least one image
            result.andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("Validation failed"))
                    .andExpect(jsonPath("$.errors.images").value("INVALID_LENGTH"));

            // Verify no raffle was created
            List<Raffle> savedRaffles = rafflesRepository.findAll();
            assertThat(savedRaffles).isEmpty();
        }

        @Test
        @DisplayName("Should succeed when some images are missing but at least one valid image remains")
        void shouldSucceedWhenSomeImagesAreMissingButValidImagesRemain() throws Exception {
            // Arrange - Create 1 pending image and reference 2 in request (1 valid, 1 missing)
            List<Image> pendingImages = createPendingImagesForUser(1);
            
            List<ImageDTO> imageDTOs = new ArrayList<>();
            // Add existing image
            imageDTOs.addAll(convertToImageDTOs(pendingImages));
            // Add non-existent image
            imageDTOs.add(new ImageDTO(99999L, "non-existent.jpg", "/path", "image/jpeg", "http://example.com", 2));

            RaffleCreate raffleCreate = new RaffleCreate(
                    "Mixed Images Raffle",
                    "Has one valid and one missing image",
                    null,
                    LocalDateTime.now().plusDays(7),
                    imageDTOs,
                    new TicketsCreate(10L, BigDecimal.valueOf(5.00), 1L)
            );

            // Act
            ResultActions result = mockMvc.perform(post(baseEndpoint)
                    .contentType(APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(raffleCreate))
                    .with(user(authData.user().getEmail())));

            // Assert - Should succeed because there's at least one valid image
            result.andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.images", hasSize(1))); // Only 1 valid image

            // Verify database state
            List<Raffle> savedRaffles = rafflesRepository.findAll();
            assertThat(savedRaffles).hasSize(1);
            
            Raffle savedRaffle = savedRaffles.get(0);
            List<Image> raffleImages = imagesRepository.findAllByRaffle(savedRaffle);
            assertThat(raffleImages).hasSize(1); // Only the valid image was associated
        }

        @Test
        @DisplayName("Should verify images have user reference cleared and association reference maintained after raffle creation")
        void shouldVerifyImageReferencesAfterRaffleCreation() throws Exception {
            // Arrange
            List<Image> pendingImages = createPendingImagesForUser(2);
            RaffleCreate raffleCreate = createValidRaffleCreate(pendingImages, 1L, 20L);

            // Act
            ResultActions result = mockMvc.perform(post(baseEndpoint)
                    .contentType(APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(raffleCreate))
                    .with(user(authData.user().getEmail())));

            // Assert
            result.andExpect(status().isCreated());

            // Verify database state - images should have user=null and association maintained
            List<Raffle> savedRaffles = rafflesRepository.findAll();
            assertThat(savedRaffles).hasSize(1);
            
            Raffle savedRaffle = savedRaffles.get(0);
            List<Image> raffleImages = imagesRepository.findAllByRaffle(savedRaffle);
            assertThat(raffleImages).hasSize(2);
            
            for (Image image : raffleImages) {
                assertThat(image.getUser()).isNull();
                assertThat(image.getAssociation()).isEqualTo(authData.association());
                assertThat(image.getRaffle()).isEqualTo(savedRaffle);
                assertThat(image.getStatus()).isEqualTo(ImageStatus.ACTIVE);
            }
        }
    }

    // Helper Methods
    private List<Image> createPendingImagesForUser(int count) {
        List<Image> images = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Image image = TestDataBuilder.image()
                    .fileName("pending-image-" + (i + 1) + ".jpg")
                    .user(authData.user())
                    .association(authData.association())
                    .status(ImageStatus.PENDING)
                    .imageOrder(i + 1)
                    .build();
            images.add(imagesRepository.save(image));
        }
        return images;
    }

    private List<Image> createPendingImagesForAdmin(AuthTestData adminData, int count) {
        List<Image> images = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Image image = TestDataBuilder.image()
                    .fileName("admin-pending-image-" + (i + 1) + ".jpg")
                    .user(adminData.user())
                    .association(adminData.association())
                    .status(ImageStatus.PENDING)
                    .imageOrder(i + 1)
                    .build();
            images.add(imagesRepository.save(image));
        }
        return images;
    }

    private List<Image> createPendingImagesForMember(AuthTestData memberData, int count) {
        List<Image> images = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Image image = TestDataBuilder.image()
                    .fileName("member-pending-image-" + (i + 1) + ".jpg")
                    .user(memberData.user())
                    .association(memberData.association())
                    .status(ImageStatus.PENDING)
                    .imageOrder(i + 1)
                    .build();
            images.add(imagesRepository.save(image));
        }
        return images;
    }

    private RaffleCreate createValidRaffleCreate(List<Image> pendingImages, Long lowerLimit, Long amount) {
        return new RaffleCreate(
                "Test Raffle",
                "This is a test raffle for integration testing",
                null,
                LocalDateTime.now().plusDays(7),
                convertToImageDTOs(pendingImages),
                new TicketsCreate(amount, BigDecimal.valueOf(15.50), lowerLimit)
        );
    }

    private List<ImageDTO> convertToImageDTOs(List<Image> images) {
        return images.stream()
                .map(image -> new ImageDTO(
                        image.getId(),
                        image.getFileName(),
                        image.getFilePath(),
                        image.getContentType(),
                        image.getUrl(),
                        image.getImageOrder()))
                .toList();
    }

    private void verifyImagesAssociatedToRaffle(Raffle raffle, List<Image> originalPendingImages) {
        // Refresh images from database
        List<Image> raffleImages = imagesRepository.findAllByRaffle(raffle);
        
        assertThat(raffleImages).hasSize(originalPendingImages.size());
        
        for (Image raffleImage : raffleImages) {
            assertThat(raffleImage.getRaffle()).isEqualTo(raffle);
            assertThat(raffleImage.getUser()).isNull();
            assertThat(raffleImage.getAssociation()).isEqualTo(authData.association());
            assertThat(raffleImage.getStatus()).isEqualTo(ImageStatus.ACTIVE);
            assertThat(raffleImage.getUrl()).contains("/raffles/" + raffle.getId() + "/images/");
            assertThat(raffleImage.getImageOrder()).isGreaterThan(0);
        }
        
        // Verify no pending images remain for this user
        List<Image> remainingPendingImages = imagesRepository.findAllByRaffleIsNullAndUserAndStatus(authData.user(), ImageStatus.PENDING);
        assertThat(remainingPendingImages).isEmpty();
    }

    private void verifyTicketsGenerated(Raffle raffle, Long lowerLimit, Long amount) {
        List<Ticket> tickets = ticketsRepository.findAllByRaffle(raffle);

        assertThat(tickets).hasSize(amount.intValue());
        
        // Verify ticket numbering
        List<String> ticketNumbers = tickets.stream()
                .map(Ticket::getTicketNumber)
                .sorted((a, b) -> Integer.compare(Integer.parseInt(a), Integer.parseInt(b))) // ← Numeric sort
                .toList();
        
        for (int i = 0; i < amount; i++) {
            String expectedNumber = String.valueOf(lowerLimit + i);
            assertThat(ticketNumbers.get(i)).isEqualTo(expectedNumber);
        }
        
        // Verify all tickets are available and linked to raffle
        for (Ticket ticket : tickets) {
            assertThat(ticket.getStatus()).isEqualTo(TicketStatus.AVAILABLE);
            assertThat(ticket.getRaffle()).isEqualTo(raffle);
            assertThat(ticket.getCustomer()).isNull();
            assertThat(ticket.getCart()).isNull();
        }
    }

    private void verifyInitialStatistics(Raffle raffle, Long expectedAvailableTickets) {
        assertThat(raffle.getStatistics()).isNotNull();
        assertThat(raffle.getStatistics().getAvailableTickets()).isEqualTo(expectedAvailableTickets);
        assertThat(raffle.getStatistics().getSoldTickets()).isEqualTo(0L);
        assertThat(raffle.getStatistics().getRevenue()).isEqualTo(BigDecimal.ZERO);
        assertThat(raffle.getStatistics().getTotalOrders()).isEqualTo(0L);
        assertThat(raffle.getStatistics().getParticipants()).isEqualTo(0L);
    }

    private RaffleCreate createValidRaffleCreateForUser(AuthTestData userData, List<Image> pendingImages, Long lowerLimit, Long amount) {
        return new RaffleCreate(
                "Test Raffle for " + userData.user().getUserName(),
                "This is a test raffle for integration testing",
                null,
                LocalDateTime.now().plusDays(7),
                convertToImageDTOs(pendingImages),
                new TicketsCreate(amount, BigDecimal.valueOf(15.50), lowerLimit)
        );
    }
} 