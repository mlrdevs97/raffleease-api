package com.raffleease.raffleease.Domains.Raffles.Controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.raffleease.raffleease.Base.AbstractIntegrationTest;
import com.raffleease.raffleease.Domains.Associations.Model.AssociationRole;
import com.raffleease.raffleease.Domains.Images.DTOs.ImageDTO;
import com.raffleease.raffleease.Domains.Images.Model.Image;
import com.raffleease.raffleease.Domains.Images.Model.ImageStatus;
import com.raffleease.raffleease.Domains.Images.Repository.ImagesRepository;
import com.raffleease.raffleease.Domains.Images.Services.FileStorageService;
import com.raffleease.raffleease.Domains.Raffles.DTOs.RaffleEdit;
import com.raffleease.raffleease.Domains.Raffles.Model.Raffle;
import com.raffleease.raffleease.Domains.Raffles.Model.RaffleStatus;
import com.raffleease.raffleease.Domains.Raffles.Model.CompletionReason;
import com.raffleease.raffleease.Domains.Raffles.Model.RaffleStatistics;
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
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.ResultActions;

import java.math.BigDecimal;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("Raffles Edit Controller Integration Tests")
class RafflesEditControllerIT extends AbstractIntegrationTest {

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
    private Raffle testRaffle;

    @BeforeEach
    void setUp() {
        authData = authTestUtils.createAuthenticatedUser();
        
        // Create a test raffle
        testRaffle = TestDataBuilder.raffle()
                .association(authData.association())
                .status(RaffleStatus.PENDING)
                .title("Original Raffle Title")
                .description("Original description")
                .build();
        testRaffle = rafflesRepository.save(testRaffle);
        
        baseEndpoint = "/v1/associations/" + authData.association().getId() + "/raffles/" + testRaffle.getId();
        
        // Mock file storage service for file movement operations
        when(fileStorageService.moveFileToRaffle(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(Paths.get("/mocked/raffle/image/path"));
    }

    @Nested
    @DisplayName("PUT /v1/associations/{associationId}/raffles/{id}")
    class EditRaffleTests {

        @Test
        @DisplayName("Should successfully edit raffle basic information")
        void shouldEditRaffleBasicInformation() throws Exception {
            // Arrange
            RaffleEdit raffleEdit = new RaffleEdit(
                    "Updated Raffle Title",
                    "Updated description",
                    null, 
                    null,   
                    null,
                    null
            );

            // Act
            ResultActions result = mockMvc.perform(put(baseEndpoint)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(raffleEdit))
                    .with(user(authData.user().getEmail())));

            // Assert
            result.andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Raffle edited successfully"))
                    .andExpect(jsonPath("$.data.title").value("Updated Raffle Title"))
                    .andExpect(jsonPath("$.data.description").value("Updated description"));

            // Verify database state
            Raffle updatedRaffle = rafflesRepository.findById(testRaffle.getId()).orElseThrow();
            assertThat(updatedRaffle.getTitle()).isEqualTo("Updated Raffle Title");
            assertThat(updatedRaffle.getDescription()).isEqualTo("Updated description");
            assertThat(updatedRaffle.getUpdatedAt()).isNotNull();
        }

        @Test
        @DisplayName("Should successfully edit raffle with new images from pending images")
        void shouldEditRaffleWithNewImagesFromPendingImages() throws Exception {
            // Arrange - Create pending images
            List<Image> pendingImages = createPendingImagesForUser(3);
            List<ImageDTO> imageDTOs = convertToImageDTOs(pendingImages);

            RaffleEdit raffleEdit = new RaffleEdit(
                    null,
                    null,
                    null,
                    imageDTOs,
                    null,
                    null
            );

            // Act
            ResultActions result = mockMvc.perform(put(baseEndpoint)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(raffleEdit))
                    .with(user(authData.user().getEmail())));

            // Assert
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.images", hasSize(3)));

            // Verify database state
            List<Image> raffleImages = imagesRepository.findAllByRaffle(testRaffle);
            assertThat(raffleImages).hasSize(3);
            
            // Verify images are properly associated
            for (Image image : raffleImages) {
                assertThat(image.getRaffle()).isEqualTo(testRaffle);
                assertThat(image.getUser()).isNull();
                assertThat(image.getAssociation()).isEqualTo(authData.association());
                assertThat(image.getStatus()).isEqualTo(ImageStatus.ACTIVE);
                assertThat(image.getUrl()).contains("/raffles/" + testRaffle.getId() + "/images/");
            }

            // Verify no pending images remain
            List<Image> remainingPendingImages = imagesRepository.findAllByRaffleIsNullAndUserAndStatus(
                authData.user(), ImageStatus.PENDING);
            assertThat(remainingPendingImages).isEmpty();
        }

        @Test
        @DisplayName("Should successfully edit raffle images by replacing existing ones")
        void shouldEditRaffleImagesByReplacingExistingOnes() throws Exception {
            // Arrange - Add some images to the raffle first
            List<Image> existingImages = createImagesForRaffle(testRaffle, 2);
            testRaffle.getImages().addAll(existingImages);
            rafflesRepository.save(testRaffle);

            // Create new pending images to replace the existing ones
            List<Image> newPendingImages = createPendingImagesForUser(2);
            List<ImageDTO> newImageDTOs = convertToImageDTOs(newPendingImages);

            RaffleEdit raffleEdit = new RaffleEdit(
                    null,
                    null,
                    null,
                    newImageDTOs,
                    null,
                    null
            );

            // Act
            ResultActions result = mockMvc.perform(put(baseEndpoint)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(raffleEdit))
                    .with(user(authData.user().getEmail())));

            // Assert
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.images", hasSize(2)));

            // Verify that the new images are associated
            List<Image> raffleImages = imagesRepository.findAllByRaffle(testRaffle);
            assertThat(raffleImages).hasSize(2);
            
            // Verify these are the new images (not the old ones)
            List<Long> newImageIds = newPendingImages.stream().map(Image::getId).toList();
            List<Long> raffleImageIds = raffleImages.stream().map(Image::getId).toList();
            assertThat(raffleImageIds).containsExactlyInAnyOrderElementsOf(newImageIds);
        }

        @Test
        @DisplayName("Should handle gracefully when some requested images are missing during edit")
        void shouldHandleGracefullyWhenSomeRequestedImagesAreMissingDuringEdit() throws Exception {
            // Arrange - Create 2 pending images but reference 3 in the request (1 missing)
            List<Image> pendingImages = createPendingImagesForUser(2);
            
            List<ImageDTO> imageDTOs = new ArrayList<>();
            // Add existing images
            imageDTOs.addAll(convertToImageDTOs(pendingImages));
            // Add non-existent image
            imageDTOs.add(new ImageDTO(99999L, "non-existent.jpg", "/path", "image/jpeg", "http://example.com", 3));

            RaffleEdit raffleEdit = new RaffleEdit(
                    null,
                    null,
                    null,
                    imageDTOs, // Mixed existing and missing images
                    null,
                    null
            );

            // Act
            ResultActions result = mockMvc.perform(put(baseEndpoint)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(raffleEdit))
                    .with(user(authData.user().getEmail())));

            // Assert - Should succeed with only the existing images (2 valid images remain)
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.images", hasSize(2))); // Only 2 existing images

            // Verify database state - only existing images were associated
            List<Image> raffleImages = imagesRepository.findAllByRaffle(testRaffle);
            assertThat(raffleImages).hasSize(2); // Only existing images were associated
        }

        @Test
        @DisplayName("Should return 400 when all requested images in edit are missing")
        void shouldReturn400WhenAllRequestedImagesInEditAreMissing() throws Exception {
            // Arrange - Add some images to the raffle first
            List<Image> existingImages = createImagesForRaffle(testRaffle, 2);
            testRaffle.getImages().addAll(existingImages);
            rafflesRepository.save(testRaffle);

            // Request only non-existent images
            List<ImageDTO> imageDTOs = List.of(
                    new ImageDTO(99998L, "missing1.jpg", "/path", "image/jpeg", "http://example.com", 1),
                    new ImageDTO(99999L, "missing2.jpg", "/path", "image/jpeg", "http://example.com", 2)
            );

            RaffleEdit raffleEdit = new RaffleEdit(
                    null,
                    null,
                    null,
                    imageDTOs, // All missing images
                    null,
                    null
            );

            // Act
            ResultActions result = mockMvc.perform(put(baseEndpoint)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(raffleEdit))
                    .with(user(authData.user().getEmail())));

            // Assert - Should fail because raffles must have at least one image
            result.andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("A raffle must have at least one image."));

            // Verify raffle still has its original images
            List<Image> raffleImages = imagesRepository.findAllByRaffle(testRaffle);
            assertThat(raffleImages).hasSize(2); // Original images still there
        }

        @Test
        @DisplayName("Should succeed when editing with some missing images but at least one valid image remains")
        void shouldSucceedWhenEditingWithSomeMissingImagesButValidImagesRemain() throws Exception {
            // Arrange - Create 1 pending image and reference 2 in edit request (1 valid, 1 missing)
            List<Image> pendingImages = createPendingImagesForUser(1);
            
            List<ImageDTO> imageDTOs = new ArrayList<>();
            // Add existing image
            imageDTOs.addAll(convertToImageDTOs(pendingImages));
            // Add non-existent image
            imageDTOs.add(new ImageDTO(99999L, "non-existent.jpg", "/path", "image/jpeg", "http://example.com", 2));

            RaffleEdit raffleEdit = new RaffleEdit(
                    null,
                    null,
                    null,
                    imageDTOs,
                    null,
                    null
            );

            // Act
            ResultActions result = mockMvc.perform(put(baseEndpoint)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(raffleEdit))
                    .with(user(authData.user().getEmail())));

            // Assert - Should succeed because there's at least one valid image
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.images", hasSize(1))); // Only 1 valid image

            // Verify database state - only the valid image was associated
            List<Image> raffleImages = imagesRepository.findAllByRaffle(testRaffle);
            assertThat(raffleImages).hasSize(1); // Only the valid image was associated
        }

        @Test
        @DisplayName("Should return 401 when user is not authenticated")
        void shouldReturn401WhenNotAuthenticated() throws Exception {
            // Arrange
            RaffleEdit raffleEdit = new RaffleEdit(
                    "Updated Title", null, null, null, null, null
            );

            // Act
            ResultActions result = mockMvc.perform(put(baseEndpoint)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(raffleEdit)));

            // Assert
            result.andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Should return 403 when user doesn't belong to association")
        void shouldReturn403WhenUserDoesntBelongToAssociation() throws Exception {
            // Arrange
            AuthTestData otherUserData = authTestUtils.createAuthenticatedUserWithCredentials(
                    "otheruser", "other@example.com", "password123");
            
            RaffleEdit raffleEdit = new RaffleEdit(
                    "Updated Title", null, null, null, null, null
            );

            // Act
            ResultActions result = mockMvc.perform(put(baseEndpoint)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(raffleEdit))
                    .with(user(otherUserData.user().getEmail())));

            // Assert
            result.andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Should return 404 when raffle doesn't exist")
        void shouldReturn404WhenRaffleDoesntExist() throws Exception {
            // Arrange
            String nonExistentRaffleEndpoint = "/v1/associations/" + authData.association().getId() + "/raffles/99999";
            RaffleEdit raffleEdit = new RaffleEdit(
                    "Updated Title", null, null, null, null, null
            );

            // Act
            ResultActions result = mockMvc.perform(put(nonExistentRaffleEndpoint)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(raffleEdit))
                    .with(user(authData.user().getEmail())));

            // Assert
            result.andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Should return 400 when trying to use images from different association")
        void shouldReturn400WhenUsingImagesFromDifferentAssociation() throws Exception {
            // Arrange
            AuthTestData otherUserData = authTestUtils.createAuthenticatedUserWithCredentials(
                    "otheruser2", "other2@example.com", "password123");
            
            // Create image belonging to different user from different association
            Image otherAssociationImage = TestDataBuilder.image()
                    .user(otherUserData.user())
                    .association(otherUserData.association())
                    .status(ImageStatus.PENDING)
                    .build();
            otherAssociationImage = imagesRepository.save(otherAssociationImage);

            RaffleEdit raffleEdit = new RaffleEdit(
                    null, null, null,
                    List.of(new ImageDTO(otherAssociationImage.getId(),
                            otherAssociationImage.getFileName(),
                            otherAssociationImage.getFilePath(),
                            otherAssociationImage.getContentType(),
                            otherAssociationImage.getUrl(),
                            1)),
                    null, null
            );

            // Act
            ResultActions result = mockMvc.perform(put(baseEndpoint)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(raffleEdit))
                    .with(user(authData.user().getEmail())));

            // Assert
            result.andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("Should successfully edit ticket price")
        void shouldEditTicketPrice() throws Exception {
            // Arrange
            BigDecimal newPrice = BigDecimal.valueOf(25.75);
            RaffleEdit raffleEdit = new RaffleEdit(
                    null, null, null, null, newPrice, null
            );

            // Act
            ResultActions result = mockMvc.perform(put(baseEndpoint)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(raffleEdit))
                    .with(user(authData.user().getEmail())));

            // Assert
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.ticketPrice").value(25.75));

            // Verify database state
            Raffle updatedRaffle = rafflesRepository.findById(testRaffle.getId()).orElseThrow();
            assertThat(updatedRaffle.getTicketPrice()).isEqualByComparingTo(newPrice);
        }

        @Test
        @DisplayName("Should successfully edit total tickets by increasing count")
        void shouldEditTotalTicketsByIncreasingCount() throws Exception {
            // Arrange - Setup raffle with initial tickets
            setupRaffleWithTickets(testRaffle, 10L, 1L);
            Long newTotal = 20L;
            
            RaffleEdit raffleEdit = new RaffleEdit(
                    null, null, null, null, null, newTotal
            );

            // Act
            ResultActions result = mockMvc.perform(put(baseEndpoint)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(raffleEdit))
                    .with(user(authData.user().getEmail())));

            // Assert
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.totalTickets").value(20));

            // Verify database state
            Raffle updatedRaffle = rafflesRepository.findById(testRaffle.getId()).orElseThrow();
            assertThat(updatedRaffle.getTotalTickets()).isEqualTo(20L);
            
            // Verify additional tickets were created
            List<Ticket> allTickets = ticketsRepository.findAllByRaffle(updatedRaffle);
            assertThat(allTickets).hasSize(20);
            
            // Verify statistics were updated
            assertThat(updatedRaffle.getStatistics().getAvailableTickets()).isEqualTo(20L);
        }

        @Test
        @DisplayName("Should successfully edit total tickets by keeping same count")
        void shouldEditTotalTicketsByKeepingSameCount() throws Exception {
            // Arrange - Setup raffle with initial tickets
            setupRaffleWithTickets(testRaffle, 10L, 1L);
            Long sameTotal = 10L;
            
            RaffleEdit raffleEdit = new RaffleEdit(
                    null, null, null, null, null, sameTotal
            );

            // Act
            ResultActions result = mockMvc.perform(put(baseEndpoint)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(raffleEdit))
                    .with(user(authData.user().getEmail())));

            // Assert
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.totalTickets").value(10));

            // Verify no additional tickets were created
            List<Ticket> allTickets = ticketsRepository.findAllByRaffle(testRaffle);
            assertThat(allTickets).hasSize(10);
        }

        @Test
        @DisplayName("Should return 400 when trying to decrease total tickets below sold count")
        void shouldReturn400WhenDecreasingTotalTicketsBelowSoldCount() throws Exception {
            // Arrange - Setup raffle with tickets, some sold
            setupRaffleWithTickets(testRaffle, 10L, 1L);
            sellTickets(testRaffle, 5); // Sell 5 tickets
            Long newTotal = 3L; // Try to set total below sold count
            
            RaffleEdit raffleEdit = new RaffleEdit(
                    null, null, null, null, null, newTotal
            );

            // Act
            ResultActions result = mockMvc.perform(put(baseEndpoint)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(raffleEdit))
                    .with(user(authData.user().getEmail())));

            // Assert
            result.andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("The total tickets count cannot be less than the number of tickets already sold for this raffle"));

            // Verify raffle remains unchanged
            Raffle unchangedRaffle = rafflesRepository.findById(testRaffle.getId()).orElseThrow();
            assertThat(unchangedRaffle.getTotalTickets()).isEqualTo(10L);
        }

        @Test
        @DisplayName("Should successfully edit end date")
        void shouldEditEndDate() throws Exception {
            // Arrange
            LocalDateTime newEndDate = LocalDateTime.now().plusDays(14);
            RaffleEdit raffleEdit = new RaffleEdit(
                    null, null, newEndDate, null, null, null
            );

            // Act
            ResultActions result = mockMvc.perform(put(baseEndpoint)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(raffleEdit))
                    .with(user(authData.user().getEmail())));

            // Assert
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));

            // Verify database state
            Raffle updatedRaffle = rafflesRepository.findById(testRaffle.getId()).orElseThrow();
            assertThat(updatedRaffle.getEndDate()).isEqualToIgnoringNanos(newEndDate);
        }

        @Test
        @DisplayName("Should reactivate completed raffle when end date is extended and completion reason was END_DATE_REACHED")
        void shouldReactivateCompletedRaffleWhenEndDateExtendedAndReasonWasEndDateReached() throws Exception {
            // Arrange - Make raffle completed due to end date reached
            setupRaffleWithTickets(testRaffle, 10L, 1L); // Add some available tickets
            testRaffle.setStatus(RaffleStatus.COMPLETED);
            testRaffle.setCompletionReason(CompletionReason.END_DATE_REACHED);
            testRaffle.setCompletedAt(LocalDateTime.now().minusDays(1));
            testRaffle.setEndDate(LocalDateTime.now().minusHours(1)); // Past end date
            rafflesRepository.save(testRaffle);

            LocalDateTime newEndDate = LocalDateTime.now().plusDays(7); // Valid future date
            RaffleEdit raffleEdit = new RaffleEdit(
                    null, null, newEndDate, null, null, null
            );

            // Act
            ResultActions result = mockMvc.perform(put(baseEndpoint)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(raffleEdit))
                    .with(user(authData.user().getEmail())));

            // Assert
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.status").value("ACTIVE"));

            // Verify raffle was reactivated
            Raffle updatedRaffle = rafflesRepository.findById(testRaffle.getId()).orElseThrow();
            assertThat(updatedRaffle.getStatus()).isEqualTo(RaffleStatus.ACTIVE);
            assertThat(updatedRaffle.getCompletionReason()).isNull();
            assertThat(updatedRaffle.getCompletedAt()).isNull();
            assertThat(updatedRaffle.getEndDate()).isEqualToIgnoringNanos(newEndDate);
        }

        @Test
        @DisplayName("Should NOT reactivate completed raffle when end date is extended but completion reason was not END_DATE_REACHED")
        void shouldNotReactivateCompletedRaffleWhenEndDateExtendedButReasonWasNotEndDateReached() throws Exception {
            // Arrange - Make raffle completed due to manual completion (not end date)
            setupRaffleWithTickets(testRaffle, 10L, 1L);
            testRaffle.setStatus(RaffleStatus.COMPLETED);
            testRaffle.setCompletionReason(CompletionReason.MANUALLY_COMPLETED); // Different reason
            testRaffle.setCompletedAt(LocalDateTime.now().minusDays(1));
            rafflesRepository.save(testRaffle);

            LocalDateTime newEndDate = LocalDateTime.now().plusDays(7);
            RaffleEdit raffleEdit = new RaffleEdit(
                    null, null, newEndDate, null, null, null
            );

            // Act
            ResultActions result = mockMvc.perform(put(baseEndpoint)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(raffleEdit))
                    .with(user(authData.user().getEmail())));

            // Assert
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.status").value("COMPLETED")); // Should remain completed

            // Verify raffle was NOT reactivated
            Raffle updatedRaffle = rafflesRepository.findById(testRaffle.getId()).orElseThrow();
            assertThat(updatedRaffle.getStatus()).isEqualTo(RaffleStatus.COMPLETED);
            assertThat(updatedRaffle.getCompletionReason()).isEqualTo(CompletionReason.MANUALLY_COMPLETED);
            assertThat(updatedRaffle.getCompletedAt()).isNotNull();
            assertThat(updatedRaffle.getEndDate()).isEqualToIgnoringNanos(newEndDate); // End date should still be updated
        }

        @Test
        @DisplayName("Should NOT reactivate raffle when end date is extended but validation fails")
        void shouldNotReactivateRaffleWhenEndDateExtendedButValidationFails() throws Exception {
            // Arrange - Make raffle completed due to end date reached, but set new end date too close
            setupRaffleWithTickets(testRaffle, 10L, 1L);
            testRaffle.setStatus(RaffleStatus.COMPLETED);
            testRaffle.setCompletionReason(CompletionReason.END_DATE_REACHED);
            testRaffle.setCompletedAt(LocalDateTime.now().minusDays(1));
            rafflesRepository.save(testRaffle);

            LocalDateTime newEndDate = LocalDateTime.now().plusHours(12); // Less than 24 hours - should fail validation
            RaffleEdit raffleEdit = new RaffleEdit(
                    null, null, newEndDate, null, null, null
            );

            // Act
            ResultActions result = mockMvc.perform(put(baseEndpoint)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(raffleEdit))
                    .with(user(authData.user().getEmail())));

            // Assert
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.status").value("COMPLETED")); // Should remain completed due to validation failure

            // Verify raffle was NOT reactivated (validation failed silently)
            Raffle updatedRaffle = rafflesRepository.findById(testRaffle.getId()).orElseThrow();
            assertThat(updatedRaffle.getStatus()).isEqualTo(RaffleStatus.COMPLETED);
            assertThat(updatedRaffle.getCompletionReason()).isEqualTo(CompletionReason.END_DATE_REACHED);
            assertThat(updatedRaffle.getCompletedAt()).isNotNull();
            assertThat(updatedRaffle.getEndDate()).isEqualToIgnoringNanos(newEndDate); // End date should still be updated
        }

        @Test
        @DisplayName("Should NOT reactivate raffle when end date is extended but no available tickets")
        void shouldNotReactivateRaffleWhenEndDateExtendedButNoAvailableTickets() throws Exception {
            // Arrange - Make raffle completed due to end date reached, but with no available tickets
            setupRaffleWithTickets(testRaffle, 5L, 1L);
            sellTickets(testRaffle, 5); // Sell all tickets
            testRaffle.setStatus(RaffleStatus.COMPLETED);
            testRaffle.setCompletionReason(CompletionReason.END_DATE_REACHED);
            testRaffle.setCompletedAt(LocalDateTime.now().minusDays(1));
            rafflesRepository.save(testRaffle);

            LocalDateTime newEndDate = LocalDateTime.now().plusDays(7); // Valid future date
            RaffleEdit raffleEdit = new RaffleEdit(
                    null, null, newEndDate, null, null, null
            );

            // Act
            ResultActions result = mockMvc.perform(put(baseEndpoint)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(raffleEdit))
                    .with(user(authData.user().getEmail())));

            // Assert
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.status").value("COMPLETED")); // Should remain completed due to no available tickets

            // Verify raffle was NOT reactivated (validation failed silently)
            Raffle updatedRaffle = rafflesRepository.findById(testRaffle.getId()).orElseThrow();
            assertThat(updatedRaffle.getStatus()).isEqualTo(RaffleStatus.COMPLETED);
            assertThat(updatedRaffle.getCompletionReason()).isEqualTo(CompletionReason.END_DATE_REACHED);
            assertThat(updatedRaffle.getCompletedAt()).isNotNull();
            assertThat(updatedRaffle.getEndDate()).isEqualToIgnoringNanos(newEndDate); // End date should still be updated
        }

        @Test
        @DisplayName("Should successfully edit end date for PENDING raffle without attempting reactivation")
        void shouldEditEndDateForPendingRaffleWithoutAttemptingReactivation() throws Exception {
            // Arrange - Raffle is in PENDING status (no start date)
            LocalDateTime newEndDate = LocalDateTime.now().plusDays(14);
            RaffleEdit raffleEdit = new RaffleEdit(
                    null, null, newEndDate, null, null, null
            );

            // Act
            ResultActions result = mockMvc.perform(put(baseEndpoint)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(raffleEdit))
                    .with(user(authData.user().getEmail())));

            // Assert
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.status").value("PENDING")); // Should remain pending

            // Verify database state
            Raffle updatedRaffle = rafflesRepository.findById(testRaffle.getId()).orElseThrow();
            assertThat(updatedRaffle.getStatus()).isEqualTo(RaffleStatus.PENDING);
            assertThat(updatedRaffle.getEndDate()).isEqualToIgnoringNanos(newEndDate);
        }

        @Test
        @DisplayName("Should successfully edit end date when it's more than 24 hours after start date")
        void shouldSuccessfullyEditEndDateWhenMoreThan24HoursAfterStartDate() throws Exception {
            // Arrange - Set raffle to ACTIVE with start date
            testRaffle.setStatus(RaffleStatus.ACTIVE);
            testRaffle.setStartDate(LocalDateTime.now().minusHours(2));
            rafflesRepository.save(testRaffle);

            LocalDateTime newEndDate = LocalDateTime.now().plusDays(2); // More than 24 hours after start
            RaffleEdit raffleEdit = new RaffleEdit(
                    null, null, newEndDate, null, null, null
            );

            // Act
            ResultActions result = mockMvc.perform(put(baseEndpoint)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(raffleEdit))
                    .with(user(authData.user().getEmail())));

            // Assert
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));

            // Verify database state
            Raffle updatedRaffle = rafflesRepository.findById(testRaffle.getId()).orElseThrow();
            assertThat(updatedRaffle.getEndDate()).isEqualToIgnoringNanos(newEndDate);
        }

        @Test
        @DisplayName("Should return 400 when trying to edit end date to less than 24 hours after start date (23h 59m)")
        void shouldReturn400WhenTryingToEditEndDateToLessThan24HoursAfterStartDate23h59m() throws Exception {
            // Arrange - Set raffle to ACTIVE with start date
            LocalDateTime startDate = LocalDateTime.now().minusHours(1);
            testRaffle.setStatus(RaffleStatus.ACTIVE);
            testRaffle.setStartDate(startDate);
            rafflesRepository.save(testRaffle);

            LocalDateTime newEndDate = startDate.plusHours(23).plusMinutes(59); // 23 hours 59 minutes after start date - should fail
            RaffleEdit raffleEdit = new RaffleEdit(
                    null, null, newEndDate, null, null, null
            );

            // Act
            ResultActions result = mockMvc.perform(put(baseEndpoint)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(raffleEdit))
                    .with(user(authData.user().getEmail())));

            // Assert
            result.andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("The end date of the raffle must be at least one day after the start date"));

            // Verify raffle end date remains unchanged
            Raffle unchangedRaffle = rafflesRepository.findById(testRaffle.getId()).orElseThrow();
            assertThat(unchangedRaffle.getEndDate()).isNotEqualTo(newEndDate);
        }

        @Test
        @DisplayName("Should return 400 when trying to edit end date to before start date")
        void shouldReturn400WhenTryingToEditEndDateToBeforeStartDate() throws Exception {
            // Arrange - Set raffle to ACTIVE with future start date
            LocalDateTime startDate = LocalDateTime.now().plusDays(2);
            testRaffle.setStatus(RaffleStatus.ACTIVE);
            testRaffle.setStartDate(startDate);
            rafflesRepository.save(testRaffle);

            LocalDateTime newEndDate = LocalDateTime.now().plusDays(1); // Future, but before start date
            RaffleEdit raffleEdit = new RaffleEdit(
                    null, null, newEndDate, null, null, null
            );

            // Act
            ResultActions result = mockMvc.perform(put(baseEndpoint)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(raffleEdit))
                    .with(user(authData.user().getEmail())));

            // Assert - Should trigger business logic validation, not DTO validation
            result.andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("The end date of the raffle must be at least one day after the start date"));

            // Verify raffle end date remains unchanged
            Raffle unchangedRaffle = rafflesRepository.findById(testRaffle.getId()).orElseThrow();
            assertThat(unchangedRaffle.getEndDate()).isNotEqualTo(newEndDate);
        }

        @Test
        @DisplayName("Should successfully edit end date for PAUSED raffle when valid")
        void shouldSuccessfullyEditEndDateForPausedRaffleWhenValid() throws Exception {
            // Arrange - Set raffle to PAUSED with start date
            LocalDateTime startDate = LocalDateTime.now().minusDays(1);
            testRaffle.setStatus(RaffleStatus.PAUSED);
            testRaffle.setStartDate(startDate);
            rafflesRepository.save(testRaffle);

            LocalDateTime newEndDate = startDate.plusDays(2); // More than 24 hours after start
            RaffleEdit raffleEdit = new RaffleEdit(
                    null, null, newEndDate, null, null, null
            );

            // Act
            ResultActions result = mockMvc.perform(put(baseEndpoint)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(raffleEdit))
                    .with(user(authData.user().getEmail())));

            // Assert
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));

            // Verify database state
            Raffle updatedRaffle = rafflesRepository.findById(testRaffle.getId()).orElseThrow();
            assertThat(updatedRaffle.getEndDate()).isEqualToIgnoringNanos(newEndDate);
            assertThat(updatedRaffle.getStatus()).isEqualTo(RaffleStatus.PAUSED); // Should remain paused
        }

        @Test
        @DisplayName("Should return 400 when trying to edit end date for PAUSED raffle to less than 24 hours after start date")
        void shouldReturn400WhenTryingToEditEndDateForPausedRaffleToLessThan24HoursAfterStartDate() throws Exception {
            // Arrange - Set raffle to PAUSED with start date
            LocalDateTime startDate = LocalDateTime.now().minusHours(2);
            testRaffle.setStatus(RaffleStatus.PAUSED);
            testRaffle.setStartDate(startDate);
            rafflesRepository.save(testRaffle);

            LocalDateTime newEndDate = startDate.plusHours(12); // Only 12 hours after start date
            RaffleEdit raffleEdit = new RaffleEdit(
                    null, null, newEndDate, null, null, null
            );

            // Act
            ResultActions result = mockMvc.perform(put(baseEndpoint)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(raffleEdit))
                    .with(user(authData.user().getEmail())));

            // Assert
            result.andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("The end date of the raffle must be at least one day after the start date"));

            // Verify raffle end date remains unchanged
            Raffle unchangedRaffle = rafflesRepository.findById(testRaffle.getId()).orElseThrow();
            assertThat(unchangedRaffle.getStatus()).isEqualTo(RaffleStatus.PAUSED);
            assertThat(unchangedRaffle.getEndDate()).isNotEqualTo(newEndDate);
        }

        @Test
        @DisplayName("Should reactivate completed raffle when total tickets increased and was completed due to all tickets sold")
        void shouldReactivateRaffleWhenTicketsIncreasedAndWasCompletedDueToAllTicketsSold() throws Exception {
            // Arrange - Setup raffle completed due to all tickets sold
            setupRaffleWithTickets(testRaffle, 5L, 1L);
            sellTickets(testRaffle, 5); // Sell all tickets
            testRaffle.setStatus(RaffleStatus.COMPLETED);
            testRaffle.setCompletionReason(CompletionReason.ALL_TICKETS_SOLD);
            testRaffle.setCompletedAt(LocalDateTime.now().minusHours(1));
            rafflesRepository.save(testRaffle);

            Long newTotal = 10L; // Increase total tickets
            RaffleEdit raffleEdit = new RaffleEdit(
                    null, null, null, null, null, newTotal
            );

            // Act
            ResultActions result = mockMvc.perform(put(baseEndpoint)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(raffleEdit))
                    .with(user(authData.user().getEmail())));

            // Assert
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.status").value("ACTIVE"));

            // Verify raffle was reactivated and tickets created
            Raffle updatedRaffle = rafflesRepository.findById(testRaffle.getId()).orElseThrow();
            assertThat(updatedRaffle.getStatus()).isEqualTo(RaffleStatus.ACTIVE);
            assertThat(updatedRaffle.getCompletionReason()).isNull();
            assertThat(updatedRaffle.getCompletedAt()).isNull();
            assertThat(updatedRaffle.getTotalTickets()).isEqualTo(10L);
            
            // Verify additional tickets were created
            List<Ticket> allTickets = ticketsRepository.findAllByRaffle(updatedRaffle);
            assertThat(allTickets).hasSize(10);
            
            // Verify statistics
            assertThat(updatedRaffle.getStatistics().getAvailableTickets()).isEqualTo(5L); // 5 new available
            assertThat(updatedRaffle.getStatistics().getSoldTickets()).isEqualTo(5L); // 5 still sold
        }

        @Test
        @DisplayName("Should not reactivate completed raffle when increasing tickets if end date is in the past")
        void shouldNotReactivateRaffleWhenIncreasingTicketsIfEndDateIsInPast() throws Exception {
            // Arrange - Setup raffle completed due to all tickets sold with past end date
            setupRaffleWithTickets(testRaffle, 5L, 1L);
            sellTickets(testRaffle, 5); // Sell all tickets
            testRaffle.setStatus(RaffleStatus.COMPLETED);
            testRaffle.setCompletionReason(CompletionReason.ALL_TICKETS_SOLD);
            testRaffle.setCompletedAt(LocalDateTime.now().minusHours(1));
            testRaffle.setEndDate(LocalDateTime.now().minusDays(1)); // Past end date
            rafflesRepository.save(testRaffle);

            Long newTotal = 10L; // Increase total tickets
            RaffleEdit raffleEdit = new RaffleEdit(
                    null, null, null, null, null, newTotal
            );

            // Act
            ResultActions result = mockMvc.perform(put(baseEndpoint)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(raffleEdit))
                    .with(user(authData.user().getEmail())));

            // Assert
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.status").value("COMPLETED"));

            // Verify raffle was NOT reactivated (remains completed)
            Raffle updatedRaffle = rafflesRepository.findById(testRaffle.getId()).orElseThrow();
            assertThat(updatedRaffle.getStatus()).isEqualTo(RaffleStatus.COMPLETED);
            assertThat(updatedRaffle.getCompletionReason()).isEqualTo(CompletionReason.END_DATE_REACHED); // Should be updated to END_DATE_REACHED
            assertThat(updatedRaffle.getCompletedAt()).isNotNull();
            assertThat(updatedRaffle.getTotalTickets()).isEqualTo(10L); // Tickets should still be increased
            
            // Verify additional tickets were created
            List<Ticket> allTickets = ticketsRepository.findAllByRaffle(updatedRaffle);
            assertThat(allTickets).hasSize(10);
            
            // Verify statistics were updated
            assertThat(updatedRaffle.getStatistics().getAvailableTickets()).isEqualTo(5L); // 5 new available
            assertThat(updatedRaffle.getStatistics().getSoldTickets()).isEqualTo(5L); // 5 still sold
        }

        @Test
        @DisplayName("Should successfully perform combined edit of multiple fields")
        void shouldPerformCombinedEditOfMultipleFields() throws Exception {
            // Arrange
            List<Image> pendingImages = createPendingImagesForUser(2);
            setupRaffleWithTickets(testRaffle, 10L, 1L);
            
            RaffleEdit raffleEdit = new RaffleEdit(
                    "New Title",
                    "New Description", 
                    LocalDateTime.now().plusDays(14),
                    convertToImageDTOs(pendingImages),
                    BigDecimal.valueOf(20.00), // New price
                    15L // Increase total tickets
            );

            // Act
            ResultActions result = mockMvc.perform(put(baseEndpoint)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(raffleEdit))
                    .with(user(authData.user().getEmail())));

            // Assert
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.title").value("New Title"))
                    .andExpect(jsonPath("$.data.description").value("New Description"))
                    .andExpect(jsonPath("$.data.totalTickets").value(15))
                    .andExpect(jsonPath("$.data.ticketPrice").value(20.00))
                    .andExpect(jsonPath("$.data.images", hasSize(2)));

            // Verify database state
            Raffle updatedRaffle = rafflesRepository.findById(testRaffle.getId()).orElseThrow();
            assertThat(updatedRaffle.getTitle()).isEqualTo("New Title");
            assertThat(updatedRaffle.getDescription()).isEqualTo("New Description");
            assertThat(updatedRaffle.getTotalTickets()).isEqualTo(15L);
            assertThat(updatedRaffle.getTicketPrice()).isEqualByComparingTo(BigDecimal.valueOf(20.00));
            
            // Verify images were associated
            List<Image> raffleImages = imagesRepository.findAllByRaffle(updatedRaffle);
            assertThat(raffleImages).hasSize(2);
            
            // Verify additional tickets were created
            List<Ticket> allTickets = ticketsRepository.findAllByRaffle(updatedRaffle);
            assertThat(allTickets).hasSize(15);
        }

        @Test
        @DisplayName("Should return 400 when end date is in the past")
        void shouldReturn400WhenEndDateIsInPast() throws Exception {
            // Arrange
            LocalDateTime pastDate = LocalDateTime.now().minusDays(1);
            RaffleEdit raffleEdit = new RaffleEdit(
                    null, null, pastDate, null, null, null
            );

            // Act
            ResultActions result = mockMvc.perform(put(baseEndpoint)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(raffleEdit))
                    .with(user(authData.user().getEmail())));

            // Assert
            result.andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("Should successfully handle mixed pending and active images during edit")
        void shouldHandleMixedPendingAndActiveImagesDuringEdit() throws Exception {
            // Arrange - Add existing active images to the raffle
            List<Image> existingActiveImages = createImagesForRaffle(testRaffle, 2);
            testRaffle.getImages().addAll(existingActiveImages);
            rafflesRepository.save(testRaffle);

            // Create new pending images
            List<Image> newPendingImages = createPendingImagesForUser(2);
            
            // Create edit request with both existing active images and new pending images
            List<ImageDTO> mixedImageDTOs = new ArrayList<>();
            
            // Add existing active images
            existingActiveImages.forEach(img -> 
                mixedImageDTOs.add(new ImageDTO(img.getId(), img.getFileName(),
                    img.getFilePath(), img.getContentType(), img.getUrl(), img.getImageOrder())));
            
            // Add new pending images
            newPendingImages.forEach(img -> 
                mixedImageDTOs.add(new ImageDTO(img.getId(), img.getFileName(), 
                    img.getFilePath(), img.getContentType(), img.getUrl(), img.getImageOrder() + 2)));

            RaffleEdit raffleEdit = new RaffleEdit(
                    null, null, null, mixedImageDTOs, null, null
            );

            // Act
            ResultActions result = mockMvc.perform(put(baseEndpoint)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(raffleEdit))
                    .with(user(authData.user().getEmail())));

            // Assert
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.images", hasSize(4)));

            // Verify database state
            List<Image> raffleImages = imagesRepository.findAllByRaffle(testRaffle);
            assertThat(raffleImages).hasSize(4);
            
            // Verify all images are now ACTIVE and have user=null
            for (Image image : raffleImages) {
                assertThat(image.getStatus()).isEqualTo(ImageStatus.ACTIVE);
                assertThat(image.getUser()).isNull(); // User reference should be cleared for all images
                assertThat(image.getAssociation()).isEqualTo(authData.association()); // Association reference maintained
                assertThat(image.getRaffle()).isEqualTo(testRaffle); // All linked to raffle
            }
            
            // Verify no pending images remain for the user
            List<Image> remainingPendingImages = imagesRepository.findAllByRaffleIsNullAndUserAndStatus(
                authData.user(), ImageStatus.PENDING);
            assertThat(remainingPendingImages).isEmpty();
        }

        @Test
        @DisplayName("Should successfully edit raffle using pending images already associated with other raffles")
        void shouldSuccessfullyEditRaffleUsingPendingImagesAlreadyAssociatedWithOtherRaffles() throws Exception {
            // Arrange - Create another raffle in the same association
            Raffle anotherRaffle = TestDataBuilder.raffle()
                    .association(authData.association())
                    .status(RaffleStatus.PENDING)
                    .title("Another Raffle")
                    .build();
            anotherRaffle = rafflesRepository.save(anotherRaffle);

            // Create pending images already associated with the other raffle
            Image pendingImageWithRaffle1 = TestDataBuilder.image()
                    .user(authData.user())
                    .association(authData.association())
                    .status(ImageStatus.PENDING)
                    .raffle(anotherRaffle)
                    .imageOrder(1)
                    .fileName("pending-with-raffle-1.jpg")
                    .build();
            Image pendingImageWithRaffle2 = TestDataBuilder.image()
                    .user(authData.user())
                    .association(authData.association())
                    .status(ImageStatus.PENDING)
                    .raffle(anotherRaffle)
                    .imageOrder(2)
                    .fileName("pending-with-raffle-2.jpg")
                    .build();
            pendingImageWithRaffle1 = imagesRepository.save(pendingImageWithRaffle1);
            pendingImageWithRaffle2 = imagesRepository.save(pendingImageWithRaffle2);

            // Create edit request using these pending images for our test raffle
            List<ImageDTO> imageDTOs = List.of(
                new ImageDTO(pendingImageWithRaffle1.getId(), pendingImageWithRaffle1.getFileName(),
                    pendingImageWithRaffle1.getFilePath(), pendingImageWithRaffle1.getContentType(),
                    pendingImageWithRaffle1.getUrl(), 1),
                new ImageDTO(pendingImageWithRaffle2.getId(), pendingImageWithRaffle2.getFileName(),
                    pendingImageWithRaffle2.getFilePath(), pendingImageWithRaffle2.getContentType(),
                    pendingImageWithRaffle2.getUrl(), 2)
            );

            RaffleEdit raffleEdit = new RaffleEdit(
                    null, null, null, imageDTOs, null, null
            );

            // Act
            ResultActions result = mockMvc.perform(put(baseEndpoint)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(raffleEdit))
                    .with(user(authData.user().getEmail())));

            // Assert - Should succeed because pending images associated with raffles are now allowed during editing
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.images", hasSize(2)));

            // Verify database state - images should now be associated with our test raffle
            List<Image> testRaffleImages = imagesRepository.findAllByRaffle(testRaffle);
            assertThat(testRaffleImages).hasSize(2);
            
            // Verify images are properly moved to our test raffle
            for (Image image : testRaffleImages) {
                assertThat(image.getStatus()).isEqualTo(ImageStatus.ACTIVE);
                assertThat(image.getUser()).isNull(); // User reference should be cleared
                assertThat(image.getAssociation()).isEqualTo(authData.association());
                assertThat(image.getRaffle()).isEqualTo(testRaffle); // Now linked to our test raffle
            }

            // Verify the other raffle no longer has these images
            List<Image> anotherRaffleImages = imagesRepository.findAllByRaffle(anotherRaffle);
            assertThat(anotherRaffleImages).isEmpty();
        }

        @Test
        @DisplayName("Should verify user reference is cleared only for newly associated pending images")
        void shouldVerifyUserReferenceHandlingForMixedImages() throws Exception {
            // Arrange - Add existing active images to the raffle (these should already have user=null)
            Image existingActiveImage = TestDataBuilder.image()
                    .user(null) // Already cleared as it's associated with raffle
                    .association(authData.association())
                    .status(ImageStatus.ACTIVE)
                    .raffle(testRaffle)
                    .imageOrder(1)
                    .fileName("existing-active.jpg")
                    .build();
            existingActiveImage = imagesRepository.save(existingActiveImage);
            testRaffle.getImages().add(existingActiveImage);
            rafflesRepository.save(testRaffle);

            // Create new pending image (has user reference)
            Image newPendingImage = TestDataBuilder.image()
                    .user(authData.user()) // Has user reference
                    .association(authData.association())
                    .status(ImageStatus.PENDING)
                    .imageOrder(2)
                    .fileName("new-pending.jpg")
                    .build();
            newPendingImage = imagesRepository.save(newPendingImage);

            // Edit with both images
            List<ImageDTO> imageDTOs = List.of(
                new ImageDTO(existingActiveImage.getId(), existingActiveImage.getFileName(), 
                    existingActiveImage.getFilePath(), existingActiveImage.getContentType(), 
                    existingActiveImage.getUrl(), 1),
                new ImageDTO(newPendingImage.getId(), newPendingImage.getFileName(), 
                    newPendingImage.getFilePath(), newPendingImage.getContentType(), 
                    newPendingImage.getUrl(), 2)
            );

            RaffleEdit raffleEdit = new RaffleEdit(null, null, null, imageDTOs, null, null);

            // Act
            ResultActions result = mockMvc.perform(put(baseEndpoint)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(raffleEdit))
                    .with(user(authData.user().getEmail())));

            // Assert
            result.andExpect(status().isOk());

            // Verify both images now have user=null and are ACTIVE
            List<Image> raffleImages = imagesRepository.findAllByRaffle(testRaffle);
            assertThat(raffleImages).hasSize(2);
            
            for (Image image : raffleImages) {
                assertThat(image.getUser()).isNull(); // Both should have user=null
                assertThat(image.getStatus()).isEqualTo(ImageStatus.ACTIVE); // Both should be ACTIVE
                assertThat(image.getRaffle()).isEqualTo(testRaffle); // Both linked to raffle
            }
        }

        @Test
        @DisplayName("Should return 403 when COLLABORATOR tries to edit raffle")
        void shouldReturn403WhenCollaboratorTriesToEditRaffle() throws Exception {
            // Arrange
            AuthTestData collaboratorData = authTestUtils.createAuthenticatedUserInSameAssociation(
                    authData.association(), AssociationRole.COLLABORATOR);
            
            RaffleEdit raffleEdit = new RaffleEdit(
                    "Updated Title", null, null, null, null, null
            );

            // Act
            ResultActions result = mockMvc.perform(put(baseEndpoint)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(raffleEdit))
                    .with(user(collaboratorData.user().getEmail())));

            // Assert
            result.andExpect(status().isForbidden())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("Only administrators and members can edit raffles"));

            // Verify raffle remains unchanged
            Raffle unchangedRaffle = rafflesRepository.findById(testRaffle.getId()).orElseThrow();
            assertThat(unchangedRaffle.getTitle()).isEqualTo("Original Raffle Title");
        }

        @Test
        @DisplayName("Should successfully edit raffle for ADMIN role")
        void shouldSuccessfullyEditRaffleForAdmin() throws Exception {
            // Arrange
            AuthTestData adminData = authTestUtils.createAuthenticatedUserInSameAssociation(
                    authData.association(), AssociationRole.ADMIN);
            
            RaffleEdit raffleEdit = new RaffleEdit(
                    "Admin Updated Title", "Admin updated description", null, null, null, null
            );

            // Act
            ResultActions result = mockMvc.perform(put(baseEndpoint)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(raffleEdit))
                    .with(user(adminData.user().getEmail())));

            // Assert
            result.andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Raffle edited successfully"))
                    .andExpect(jsonPath("$.data.title").value("Admin Updated Title"))
                    .andExpect(jsonPath("$.data.description").value("Admin updated description"));

            // Verify database state
            Raffle updatedRaffle = rafflesRepository.findById(testRaffle.getId()).orElseThrow();
            assertThat(updatedRaffle.getTitle()).isEqualTo("Admin Updated Title");
            assertThat(updatedRaffle.getDescription()).isEqualTo("Admin updated description");
        }

        @Test
        @DisplayName("Should successfully edit raffle for MEMBER role")
        void shouldSuccessfullyEditRaffleForMember() throws Exception {
            // Arrange
            AuthTestData memberData = authTestUtils.createAuthenticatedUserInSameAssociation(
                    authData.association(), AssociationRole.MEMBER);
            
            RaffleEdit raffleEdit = new RaffleEdit(
                    "Member Updated Title", "Member updated description", null, null, null, null
            );

            // Act
            ResultActions result = mockMvc.perform(put(baseEndpoint)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(raffleEdit))
                    .with(user(memberData.user().getEmail())));

            // Assert
            result.andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Raffle edited successfully"))
                    .andExpect(jsonPath("$.data.title").value("Member Updated Title"))
                    .andExpect(jsonPath("$.data.description").value("Member updated description"));

            // Verify database state
            Raffle updatedRaffle = rafflesRepository.findById(testRaffle.getId()).orElseThrow();
            assertThat(updatedRaffle.getTitle()).isEqualTo("Member Updated Title");
            assertThat(updatedRaffle.getDescription()).isEqualTo("Member updated description");
        }

        @Test
        @DisplayName("Should successfully edit end date when exactly 24 hours after start date")
        void shouldSuccessfullyEditEndDateWhenExactly24HoursAfterStartDate() throws Exception {
            // Arrange - Set raffle to ACTIVE with start date
            LocalDateTime startDate = LocalDateTime.now().minusHours(1);
            testRaffle.setStatus(RaffleStatus.ACTIVE);
            testRaffle.setStartDate(startDate);
            rafflesRepository.save(testRaffle);

            LocalDateTime newEndDate = startDate.plusHours(24); // Exactly 24 hours after start date - should be allowed
            RaffleEdit raffleEdit = new RaffleEdit(
                    null, null, newEndDate, null, null, null
            );

            // Act
            ResultActions result = mockMvc.perform(put(baseEndpoint)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(raffleEdit))
                    .with(user(authData.user().getEmail())));

            // Assert
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));

            // Verify database state - end date should be updated successfully
            Raffle updatedRaffle = rafflesRepository.findById(testRaffle.getId()).orElseThrow();
            assertThat(updatedRaffle.getEndDate()).isEqualToIgnoringNanos(newEndDate);
        }

        @Test
        @DisplayName("Should return 400 when trying to edit end date to future time but less than 24 hours after start date")
        void shouldReturn400WhenTryingToEditEndDateToFutureButLessThan24HoursAfterStartDate() throws Exception {
            // Arrange - Set raffle to ACTIVE with start date in the past
            LocalDateTime startDate = LocalDateTime.now().minusHours(2);
            testRaffle.setStatus(RaffleStatus.ACTIVE);
            testRaffle.setStartDate(startDate);
            rafflesRepository.save(testRaffle);

            LocalDateTime newEndDate = LocalDateTime.now().plusHours(12); // Future, but less than 24 hours after start date
            RaffleEdit raffleEdit = new RaffleEdit(
                    null, null, newEndDate, null, null, null
            );

            // Act
            ResultActions result = mockMvc.perform(put(baseEndpoint)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(raffleEdit))
                    .with(user(authData.user().getEmail())));

            // Assert
            result.andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("The end date of the raffle must be at least one day after the start date"));

            // Verify raffle end date remains unchanged
            Raffle unchangedRaffle = rafflesRepository.findById(testRaffle.getId()).orElseThrow();
            assertThat(unchangedRaffle.getEndDate()).isNotEqualTo(newEndDate);
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

    private List<Image> createImagesForRaffle(Raffle raffle, int count) {
        List<Image> images = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Image image = TestDataBuilder.image()
                    .fileName("raffle-image-" + (i + 1) + ".jpg")
                    .user(authData.user())
                    .association(authData.association())
                    .status(ImageStatus.ACTIVE)
                    .raffle(raffle)
                    .imageOrder(i + 1)
                    .build();
            images.add(imagesRepository.save(image));
        }
        return images;
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

    private void setupRaffleWithTickets(Raffle raffle, Long totalTickets, Long firstTicketNumber) {
        // Set raffle properties
        raffle.setTotalTickets(totalTickets);
        raffle.setFirstTicketNumber(firstTicketNumber);
        
        // Initialize statistics if not present
        if (raffle.getStatistics() == null) {
            raffle.setStatistics(RaffleStatistics.builder()
                    .availableTickets(totalTickets)
                    .soldTickets(0L)
                    .revenue(BigDecimal.ZERO)
                    .totalOrders(0L)
                    .participants(0L)
                    .build());
        } else {
            raffle.getStatistics().setAvailableTickets(totalTickets);
            raffle.getStatistics().setSoldTickets(0L);
        }
        
        rafflesRepository.save(raffle);
        
        // Create tickets
        for (long i = 0; i < totalTickets; i++) {
            Ticket ticket = Ticket.builder()
                    .raffle(raffle)
                    .ticketNumber(String.valueOf(firstTicketNumber + i))
                    .status(TicketStatus.AVAILABLE)
                    .build();
            ticketsRepository.save(ticket);
        }
    }

    private void sellTickets(Raffle raffle, int count) {
        List<Ticket> availableTickets = ticketsRepository.findByRaffleAndStatus(raffle, TicketStatus.AVAILABLE);
        
        for (int i = 0; i < count && i < availableTickets.size(); i++) {
            Ticket ticket = availableTickets.get(i);
            ticket.setStatus(TicketStatus.SOLD);
            ticketsRepository.save(ticket);
        }
        
        // Update statistics
        raffle.getStatistics().setSoldTickets((long) count);
        raffle.getStatistics().setAvailableTickets(raffle.getTotalTickets() - count);
        rafflesRepository.save(raffle);
    }
} 