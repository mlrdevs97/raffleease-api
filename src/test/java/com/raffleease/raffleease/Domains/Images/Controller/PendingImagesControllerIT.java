package com.raffleease.raffleease.Domains.Images.Controller;

import com.raffleease.raffleease.Base.AbstractIntegrationTest;
import com.raffleease.raffleease.Domains.Associations.Model.AssociationRole;
import com.raffleease.raffleease.Domains.Images.Model.Image;
import com.raffleease.raffleease.Domains.Images.Model.ImageStatus;
import com.raffleease.raffleease.Domains.Images.Repository.ImagesRepository;
import com.raffleease.raffleease.Domains.Images.Services.FileStorageService;
import com.raffleease.raffleease.Domains.Raffles.Model.Raffle;
import com.raffleease.raffleease.Domains.Raffles.Model.RaffleStatus;
import com.raffleease.raffleease.Domains.Raffles.Repository.RafflesRepository;
import com.raffleease.raffleease.util.AuthTestUtils;
import com.raffleease.raffleease.util.AuthTestUtils.AuthTestData;
import com.raffleease.raffleease.util.TestDataBuilder;
import com.raffleease.raffleease.Common.Exceptions.CustomExceptions.FileStorageException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.reset;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("Pending Images Controller Integration Tests")
class PendingImagesControllerIT extends AbstractIntegrationTest {

    @Autowired
    private AuthTestUtils authTestUtils;

    @Autowired
    private ImagesRepository imagesRepository;

    @Autowired
    private RafflesRepository rafflesRepository;

    @MockitoBean
    private FileStorageService fileStorageService;

    private AuthTestData authData;
    private String baseEndpoint;

    @BeforeEach
    void setUp() {
        authData = authTestUtils.createAuthenticatedUser();
        baseEndpoint = "/v1/associations/" + authData.association().getId() + "/images";
        
        // Mock new batch file storage methods for pending images (no raffle, so null raffleId)
        when(fileStorageService.saveTemporaryBatch(anyList(), anyString(), anyString()))
                .thenAnswer(invocation -> {
                    List<MultipartFile> files = invocation.getArgument(0);
                    return files.stream()
                            .map(file -> "/mocked/temp/pending/" + file.getOriginalFilename())
                            .toList();
                });
        
        when(fileStorageService.moveTemporaryBatchToFinal(anyList(), anyString(), isNull(), anyList()))
                .thenAnswer(invocation -> {
                    List<String> tempPaths = invocation.getArgument(0);
                    return tempPaths.stream()
                            .map(path -> path.replace("/temp/", "/final/"))
                            .toList();
                });
    }

    @Nested
    @DisplayName("DELETE /v1/associations/{associationId}/images/{id}")
    class DeleteImageTests {

        @Test
        @DisplayName("Should successfully soft delete pending image when authenticated and authorized")
        void shouldSoftDeletePendingImageWhenAuthenticatedAndAuthorized() throws Exception {
            // Arrange
            Image testImage = TestDataBuilder.image()
                    .user(authData.user())
                    .association(authData.association())
                    .status(ImageStatus.PENDING)
                    .fileName("pending-image.jpg")
                    .build();
            testImage = imagesRepository.save(testImage);

            // Act
            ResultActions result = mockMvc.perform(delete(baseEndpoint + "/" + testImage.getId())
                    .with(user(authData.user().getEmail())));

            // Assert
            result.andExpect(status().isNoContent());

            // Verify image status is updated to MARKED_FOR_DELETION
            Image updatedImage = imagesRepository.findById(testImage.getId()).orElseThrow();
            assertThat(updatedImage.getStatus()).isEqualTo(ImageStatus.MARKED_FOR_DELETION);
        }

        @Test
        @DisplayName("Should return 401 when user is not authenticated")
        void shouldReturn401WhenNotAuthenticated() throws Exception {
            // Arrange
            Image testImage = TestDataBuilder.image()
                    .user(authData.user())
                    .association(authData.association())
                    .status(ImageStatus.PENDING)
                    .fileName("pending-image.jpg")
                    .build();
            testImage = imagesRepository.save(testImage);

            // Act
            ResultActions result = mockMvc.perform(delete(baseEndpoint + "/" + testImage.getId()));

            // Assert
            result.andExpect(status().isUnauthorized());

            // Verify image status is unchanged
            Image unchangedImage = imagesRepository.findById(testImage.getId()).orElseThrow();
            assertThat(unchangedImage.getStatus()).isEqualTo(ImageStatus.PENDING);
        }

        @Test
        @DisplayName("Should return 403 when user doesn't belong to association")
        void shouldReturn403WhenUserDoesntBelongToAssociation() throws Exception {
            // Arrange
            AuthTestData otherUserData = authTestUtils.createAuthenticatedUserWithCredentials(
                    "otheruser", "other@example.com", "password123");
            Image testImage = TestDataBuilder.image()
                    .user(authData.user())
                    .association(authData.association())
                    .status(ImageStatus.PENDING)
                    .fileName("pending-image.jpg")
                    .build();
            testImage = imagesRepository.save(testImage);

            // Act
            ResultActions result = mockMvc.perform(delete(baseEndpoint + "/" + testImage.getId())
                    .with(user(otherUserData.user().getEmail())));

            // Assert
            result.andExpect(status().isForbidden());

            // Verify image status is unchanged
            Image unchangedImage = imagesRepository.findById(testImage.getId()).orElseThrow();
            assertThat(unchangedImage.getStatus()).isEqualTo(ImageStatus.PENDING);
        }

        @Test
        @DisplayName("Should return 404 when image doesn't exist")
        void shouldReturn404WhenImageDoesntExist() throws Exception {
            // Act
            ResultActions result = mockMvc.perform(delete(baseEndpoint + "/99999")
                    .with(user(authData.user().getEmail())));

            // Assert
            result.andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Should return 403 when COLLABORATOR tries to delete pending image")
        void shouldReturn403WhenCollaboratorTriesToDeletePendingImage() throws Exception {
            // Arrange
            AuthTestData collaboratorData = authTestUtils.createAuthenticatedUserInSameAssociation(
                    authData.association(), AssociationRole.COLLABORATOR);
            Image testImage = TestDataBuilder.image()
                    .user(authData.user())
                    .association(authData.association())
                    .status(ImageStatus.PENDING)
                    .fileName("pending-image.jpg")
                    .build();
            testImage = imagesRepository.save(testImage);

            // Act
            ResultActions result = mockMvc.perform(delete(baseEndpoint + "/" + testImage.getId())
                    .with(user(collaboratorData.user().getEmail())));

            // Assert
            result.andExpect(status().isForbidden())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("Only administrators and members can delete images"));

            // Verify image status is unchanged
            Image unchangedImage = imagesRepository.findById(testImage.getId()).orElseThrow();
            assertThat(unchangedImage.getStatus()).isEqualTo(ImageStatus.PENDING);
        }

        @Test
        @DisplayName("Should successfully delete pending image for ADMIN role")
        void shouldSuccessfullyDeletePendingImageForAdmin() throws Exception {
            // Arrange
            AuthTestData adminData = authTestUtils.createAuthenticatedUserInSameAssociation(
                    authData.association(), AssociationRole.ADMIN);
            Image testImage = TestDataBuilder.image()
                    .user(authData.user())
                    .association(authData.association())
                    .status(ImageStatus.PENDING)
                    .fileName("admin-delete-pending.jpg")
                    .build();
            testImage = imagesRepository.save(testImage);

            // Act
            ResultActions result = mockMvc.perform(delete(baseEndpoint + "/" + testImage.getId())
                    .with(user(adminData.user().getEmail())));

            // Assert
            result.andExpect(status().isNoContent());

            // Verify image status is updated to MARKED_FOR_DELETION
            Image updatedImage = imagesRepository.findById(testImage.getId()).orElseThrow();
            assertThat(updatedImage.getStatus()).isEqualTo(ImageStatus.MARKED_FOR_DELETION);
        }

        @Test
        @DisplayName("Should successfully delete pending image for MEMBER role")
        void shouldSuccessfullyDeletePendingImageForMember() throws Exception {
            // Arrange
            AuthTestData memberData = authTestUtils.createAuthenticatedUserInSameAssociation(
                    authData.association(), AssociationRole.MEMBER);
            Image testImage = TestDataBuilder.image()
                    .user(authData.user())
                    .association(authData.association())
                    .status(ImageStatus.PENDING)
                    .fileName("member-delete-pending.jpg")
                    .build();
            testImage = imagesRepository.save(testImage);

            // Act
            ResultActions result = mockMvc.perform(delete(baseEndpoint + "/" + testImage.getId())
                    .with(user(memberData.user().getEmail())));

            // Assert
            result.andExpect(status().isNoContent());

            // Verify image status is updated to MARKED_FOR_DELETION
            Image updatedImage = imagesRepository.findById(testImage.getId()).orElseThrow();
            assertThat(updatedImage.getStatus()).isEqualTo(ImageStatus.MARKED_FOR_DELETION);
        }

        @Test
        @DisplayName("Should successfully soft delete active image through pending endpoint")
        void shouldSoftDeleteActiveImageThroughPendingEndpoint() throws Exception {
            // Arrange - Test that active images can also be soft deleted through this endpoint
            Image testImage = TestDataBuilder.image()
                    .user(authData.user())
                    .association(authData.association())
                    .status(ImageStatus.ACTIVE)
                    .fileName("active-image.jpg")
                    .build();
            testImage = imagesRepository.save(testImage);

            // Act
            ResultActions result = mockMvc.perform(delete(baseEndpoint + "/" + testImage.getId())
                    .with(user(authData.user().getEmail())));

            // Assert
            result.andExpect(status().isNoContent());

            // Verify image status is updated to MARKED_FOR_DELETION
            Image updatedImage = imagesRepository.findById(testImage.getId()).orElseThrow();
            assertThat(updatedImage.getStatus()).isEqualTo(ImageStatus.MARKED_FOR_DELETION);
        }

        @Test
        @DisplayName("Should prevent multiple soft deletes on same image")
        void shouldPreventMultipleSoftDeletesOnSameImage() throws Exception {
            // Arrange
            Image testImage = TestDataBuilder.image()
                    .user(authData.user())
                    .association(authData.association())
                    .status(ImageStatus.PENDING)
                    .fileName("pending-image.jpg")
                    .build();
            testImage = imagesRepository.save(testImage);

            // Act - First soft delete (should succeed)
            mockMvc.perform(delete(baseEndpoint + "/" + testImage.getId())
                    .with(user(authData.user().getEmail())))
                    .andExpect(status().isNoContent());

            // Act - Second soft delete attempt (should still work but image already marked)
            ResultActions result = mockMvc.perform(delete(baseEndpoint + "/" + testImage.getId())
                    .with(user(authData.user().getEmail())));

            // Assert - Should still return success (idempotent operation)
            result.andExpect(status().isNoContent());

            // Verify image status remains MARKED_FOR_DELETION
            Image updatedImage = imagesRepository.findById(testImage.getId()).orElseThrow();
            assertThat(updatedImage.getStatus()).isEqualTo(ImageStatus.MARKED_FOR_DELETION);
        }
    }

    @Nested
    @DisplayName("POST /v1/associations/{associationId}/images")
    class UploadImagesTests {

        @Test
        @DisplayName("Should successfully upload pending images when authenticated")
        void shouldUploadPendingImagesWhenAuthenticated() throws Exception {
            // Arrange
            MockMultipartFile imageFile = new MockMultipartFile(
                    "files", 
                    "test-image.jpg", 
                    "image/jpeg", 
                    createTestImageContent()
            );

            // Act
            ResultActions result = mockMvc.perform(multipart(baseEndpoint)
                    .file(imageFile)
                    .with(user(authData.user().getEmail())));

            // Assert
            result.andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("New images created successfully"))
                    .andExpect(jsonPath("$.data.images").isArray())
                    .andExpect(jsonPath("$.data.images[0].fileName").value("test-image.jpg"))
                    .andExpect(jsonPath("$.data.images[0].contentType").value("image/jpeg"))
                    .andExpect(jsonPath("$.data.images[0].url").exists())
                    .andExpect(jsonPath("$.data.images[0].imageOrder").value(1));

            List<Image> savedImages = imagesRepository.findAllByRaffleIsNullAndUserAndStatus(
                authData.user(), ImageStatus.PENDING);
            assertThat(savedImages).hasSize(1);
            assertThat(savedImages.get(0).getFileName()).isEqualTo("test-image.jpg");
            assertThat(savedImages.get(0).getUser().getId()).isEqualTo(authData.user().getId());
            assertThat(savedImages.get(0).getStatus()).isEqualTo(ImageStatus.PENDING);
            assertThat(savedImages.get(0).getRaffle()).isNull(); // Pending image
        }

        @Test
        @DisplayName("Should return 401 when user is not authenticated")
        void shouldReturn401WhenNotAuthenticated() throws Exception {
            // Arrange
            MockMultipartFile imageFile = new MockMultipartFile(
                    "files", 
                    "test.jpg", 
                    "image/jpeg", 
                    createTestImageContent()
            );

            // Act
            ResultActions result = mockMvc.perform(multipart(baseEndpoint)
                    .file(imageFile));

            // Assert
            result.andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Should return 403 when user doesn't belong to association")
        void shouldReturn403WhenUserDoesntBelongToAssociation() throws Exception {
            // Arrange
            AuthTestData otherUserData = authTestUtils.createAuthenticatedUserWithCredentials(
                    "otheruser", "other@example.com", "password123");
            MockMultipartFile imageFile = new MockMultipartFile(
                    "files", 
                    "test.jpg", 
                    "image/jpeg", 
                    createTestImageContent()
            );

            // Act
            ResultActions result = mockMvc.perform(multipart(baseEndpoint)
                    .file(imageFile)
                    .with(user(otherUserData.user().getEmail())));

            // Assert
            result.andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Should return 403 when COLLABORATOR tries to upload pending images")
        void shouldReturn403WhenCollaboratorTriesToUploadPendingImages() throws Exception {
            // Arrange
            AuthTestData collaboratorData = authTestUtils.createAuthenticatedUserInSameAssociation(
                    authData.association(), AssociationRole.COLLABORATOR);
            MockMultipartFile imageFile = new MockMultipartFile(
                    "files", 
                    "collaborator-test.jpg", 
                    "image/jpeg", 
                    createTestImageContent()
            );

            // Act
            ResultActions result = mockMvc.perform(multipart(baseEndpoint)
                    .file(imageFile)
                    .with(user(collaboratorData.user().getEmail())));

            // Assert
            result.andExpect(status().isForbidden())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("Only administrators and members can upload images"));
        }

        @Test
        @DisplayName("Should successfully upload pending images for ADMIN role")
        void shouldSuccessfullyUploadPendingImagesForAdmin() throws Exception {
            // Arrange
            AuthTestData adminData = authTestUtils.createAuthenticatedUserInSameAssociation(
                    authData.association(), AssociationRole.ADMIN);
            MockMultipartFile imageFile = new MockMultipartFile(
                    "files", 
                    "admin-test.jpg", 
                    "image/jpeg", 
                    createTestImageContent()
            );

            // Act
            ResultActions result = mockMvc.perform(multipart(baseEndpoint)
                    .file(imageFile)
                    .with(user(adminData.user().getEmail())));

            // Assert
            result.andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("New images created successfully"));
        }

        @Test
        @DisplayName("Should successfully upload pending images for MEMBER role")
        void shouldSuccessfullyUploadPendingImagesForMember() throws Exception {
            // Arrange
            AuthTestData memberData = authTestUtils.createAuthenticatedUserInSameAssociation(
                    authData.association(), AssociationRole.MEMBER);
            MockMultipartFile imageFile = new MockMultipartFile(
                    "files", 
                    "member-test.jpg", 
                    "image/jpeg", 
                    createTestImageContent()
            );

            // Act
            ResultActions result = mockMvc.perform(multipart(baseEndpoint)
                    .file(imageFile)
                    .with(user(memberData.user().getEmail())));

            // Assert
            result.andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("New images created successfully"));
        }

        @Test
        @DisplayName("Should return 400 when files exceed existing pending images limit")
        void shouldReturn400WhenExceedingImageLimit() throws Exception {
            // Arrange - Create 8 existing pending images for the authenticated user
            for (int i = 0; i < 8; i++) {
                Image existingImage = TestDataBuilder.image()
                        .fileName("existing" + i + ".jpg")
                        .user(authData.user())
                        .association(authData.association())
                        .status(ImageStatus.PENDING)
                        .imageOrder(i + 1)
                        .build();
                imagesRepository.save(existingImage);
            }

            // Try to upload 3 more files separately to simulate exceeding limit
            MockMultipartFile image1 = new MockMultipartFile("files", "new1.jpg", "image/jpeg", createTestImageContent());
            MockMultipartFile image2 = new MockMultipartFile("files", "new2.jpg", "image/jpeg", createTestImageContent());
            MockMultipartFile image3 = new MockMultipartFile("files", "new3.jpg", "image/jpeg", createTestImageContent());

            // Act
            ResultActions result = mockMvc.perform(multipart(baseEndpoint)
                    .file(image1)
                    .file(image2)
                    .file(image3)
                    .with(user(authData.user().getEmail())));

            // Assert
            result.andExpect(status().isBadRequest())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("You cannot upload more than 10 images in total"));
        }

        @Test
        @DisplayName("Should return 400 when no files are provided")
        void shouldReturn400WhenNoFilesProvided() throws Exception {
            // Act
            ResultActions result = mockMvc.perform(multipart(baseEndpoint)
                    .with(user(authData.user().getEmail())));

            // Assert
            result.andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should not create database records when file storage batch save fails")
        void shouldNotCreateDatabaseRecordsWhenFileStorageBatchSaveFails() throws Exception {
            // Arrange - Create existing pending images to get close to limit
            for (int i = 0; i < 9; i++) {
                Image existingImage = TestDataBuilder.image()
                        .fileName("existing" + i + ".jpg")
                        .user(authData.user())
                        .association(authData.association())
                        .status(ImageStatus.PENDING)
                        .imageOrder(i + 1)
                        .build();
                imagesRepository.save(existingImage);
            }
            
            // Reset and reconfigure mock to throw exception after validation passes
            reset(fileStorageService);
            when(fileStorageService.saveTemporaryBatch(anyList(), anyString(), anyString()))
                    .thenThrow(new FileStorageException("Simulated batch storage failure"));

            MockMultipartFile imageFile = new MockMultipartFile(
                    "files", 
                    "test-image.jpg", 
                    "image/jpeg", 
                    createTestImageContent()
            );

            // Count images before the operation
            List<Image> imagesBefore = imagesRepository.findAllByRaffleIsNullAndUserAndStatus(
                authData.user(), ImageStatus.PENDING);
            int countBefore = imagesBefore.size();

            // Act
            ResultActions result = mockMvc.perform(multipart(baseEndpoint)
                    .file(imageFile)
                    .with(user(authData.user().getEmail())));

            // Assert - Operation should fail completely
            result.andExpect(status().isInternalServerError())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("Simulated batch storage failure"))
                    .andExpect(jsonPath("$.code").value("FILE_STORAGE_ERROR"));

            // Verify database state is unchanged - no new records should be created
            List<Image> imagesAfter = imagesRepository.findAllByRaffleIsNullAndUserAndStatus(
                authData.user(), ImageStatus.PENDING);
            assertThat(imagesAfter).hasSize(countBefore);
            
            // Verify the specific file was not saved
            List<Image> allImages = imagesRepository.findAll();
            assertThat(allImages.stream().anyMatch(img -> "test-image.jpg".equals(img.getFileName()))).isFalse();
        }

        @Test
        @DisplayName("Should not create database records when file move batch fails")
        void shouldNotCreateDatabaseRecordsWhenFileMoveBatchFails() throws Exception {
            // Arrange - Reset and reconfigure mock to throw exception during move phase
            reset(fileStorageService);
            when(fileStorageService.saveTemporaryBatch(anyList(), anyString(), anyString()))
                    .thenReturn(List.of("/mocked/temp/path"));
            when(fileStorageService.moveTemporaryBatchToFinal(anyList(), anyString(), isNull(), anyList()))
                    .thenThrow(new FileStorageException("Simulated batch move failure"));

            MockMultipartFile imageFile = new MockMultipartFile(
                    "files", 
                    "test-image.jpg", 
                    "image/jpeg", 
                    createTestImageContent()
            );

            // Count images before the operation
            List<Image> imagesBefore = imagesRepository.findAllByRaffleIsNullAndUserAndStatus(
                authData.user(), ImageStatus.PENDING);
            int countBefore = imagesBefore.size();

            // Act
            ResultActions result = mockMvc.perform(multipart(baseEndpoint)
                    .file(imageFile)
                    .with(user(authData.user().getEmail())));

            // Assert - Operation should fail completely
            result.andExpect(status().isInternalServerError())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("Simulated batch move failure"))
                    .andExpect(jsonPath("$.code").value("FILE_STORAGE_ERROR"));

            // Verify database state is unchanged - no new records should be created
            List<Image> imagesAfter = imagesRepository.findAllByRaffleIsNullAndUserAndStatus(
                authData.user(), ImageStatus.PENDING);
            assertThat(imagesAfter).hasSize(countBefore);
            
            // Verify the specific file was not saved
            List<Image> allImages = imagesRepository.findAll();
            assertThat(allImages.stream().anyMatch(img -> "test-image.jpg".equals(img.getFileName()))).isFalse();
        }

        @Test
        @DisplayName("Should allow association to have more than 10 pending images when they belong to different users")
        void shouldAllowAssociationToHaveMoreThan10PendingImagesFromDifferentUsers() throws Exception {
            // Arrange - Create another user in the same association
            AuthTestData secondUserData = authTestUtils.createAuthenticatedUserInSameAssociation(authData.association());
            
            // Create 10 pending images for first user (at limit)
            for (int i = 0; i < 10; i++) {
                Image existingImage = TestDataBuilder.image()
                        .fileName("user1-image" + i + ".jpg")
                        .user(authData.user())
                        .association(authData.association())
                        .status(ImageStatus.PENDING)
                        .imageOrder(i + 1)
                        .build();
                imagesRepository.save(existingImage);
            }

            // Create 5 pending images for second user
            for (int i = 0; i < 5; i++) {
                Image existingImage = TestDataBuilder.image()
                        .fileName("user2-image" + i + ".jpg")
                        .user(secondUserData.user())
                        .association(authData.association())
                        .status(ImageStatus.PENDING)
                        .imageOrder(i + 1)
                        .build();
                imagesRepository.save(existingImage);
            }

            // Now second user should be able to upload 5 more images (reaching their limit of 10)
            MockMultipartFile image1 = new MockMultipartFile("files", "new1.jpg", "image/jpeg", createTestImageContent());
            MockMultipartFile image2 = new MockMultipartFile("files", "new2.jpg", "image/jpeg", createTestImageContent());
            MockMultipartFile image3 = new MockMultipartFile("files", "new3.jpg", "image/jpeg", createTestImageContent());
            MockMultipartFile image4 = new MockMultipartFile("files", "new4.jpg", "image/jpeg", createTestImageContent());
            MockMultipartFile image5 = new MockMultipartFile("files", "new5.jpg", "image/jpeg", createTestImageContent());

            String secondUserEndpoint = "/v1/associations/" + authData.association().getId() + "/images";

            // Act - Second user uploads 5 more images
            ResultActions result = mockMvc.perform(multipart(secondUserEndpoint)
                    .file(image1)
                    .file(image2)
                    .file(image3)
                    .file(image4)
                    .file(image5)
                    .with(user(secondUserData.user().getEmail())));

            // Assert - Should succeed because second user is within their 10-image limit
            result.andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.images").isArray())
                    .andExpect(jsonPath("$.data.images", hasSize(5)));

            // Verify database state - association now has 20 pending images total
            List<Image> allPendingImages = imagesRepository.findAll().stream()
                    .filter(img -> img.getStatus() == ImageStatus.PENDING && img.getAssociation().equals(authData.association()))
                    .toList();
            assertThat(allPendingImages).hasSize(20); // 10 from user1 + 10 from user2

            // Verify each user has exactly 10 pending images
            List<Image> user1PendingImages = imagesRepository.findAllByRaffleIsNullAndUserAndStatus(
                authData.user(), ImageStatus.PENDING);
            assertThat(user1PendingImages).hasSize(10);

            List<Image> user2PendingImages = imagesRepository.findAllByRaffleIsNullAndUserAndStatus(
                secondUserData.user(), ImageStatus.PENDING);
            assertThat(user2PendingImages).hasSize(10);
        }

        @Test
        @DisplayName("Should prevent user from exceeding their 10-image limit even when association has fewer images")
        void shouldPreventUserFromExceedingPersonalLimitEvenWhenAssociationHasFewerImages() throws Exception {
            // Arrange - Create 9 pending images for the user (just under limit)
            for (int i = 0; i < 9; i++) {
                Image existingImage = TestDataBuilder.image()
                        .fileName("existing" + i + ".jpg")
                        .user(authData.user())
                        .association(authData.association())
                        .status(ImageStatus.PENDING)
                        .imageOrder(i + 1)
                        .build();
                imagesRepository.save(existingImage);
            }

            // Try to upload 2 more files (would exceed user's 10-image limit)
            MockMultipartFile image1 = new MockMultipartFile("files", "new1.jpg", "image/jpeg", createTestImageContent());
            MockMultipartFile image2 = new MockMultipartFile("files", "new2.jpg", "image/jpeg", createTestImageContent());

            // Act
            ResultActions result = mockMvc.perform(multipart(baseEndpoint)
                    .file(image1)
                    .file(image2)
                    .with(user(authData.user().getEmail())));

            // Assert - Should fail because user would exceed their 10-image limit
            result.andExpect(status().isBadRequest())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("You cannot upload more than 10 images in total"));

            // Verify no new images were created
            List<Image> userPendingImages = imagesRepository.findAllByRaffleIsNullAndUserAndStatus(
                authData.user(), ImageStatus.PENDING);
            assertThat(userPendingImages).hasSize(9); // Still 9, no new images added
        }
    }

    @Nested
    @DisplayName("GET /v1/associations/{associationId}/images/images")
    class GetUserImagesTests {

        @Test
        @DisplayName("Should successfully return only user's pending images")
        void shouldReturnOnlyUserPendingImages() throws Exception {
            // Arrange - Create various images for the authenticated user
            Image pendingImage = TestDataBuilder.image()
                    .user(authData.user())
                    .association(authData.association())
                    .status(ImageStatus.PENDING)
                    .fileName("pending-image.jpg")
                    .imageOrder(1)
                    .url("http://example.com/pending.jpg")
                    .build();
            Image activeImage = TestDataBuilder.image()
                    .user(authData.user())
                    .association(authData.association())
                    .status(ImageStatus.ACTIVE)
                    .fileName("active-image.jpg")
                    .imageOrder(2)
                    .url("http://example.com/active.jpg")
                    .build();
            imagesRepository.saveAll(List.of(pendingImage, activeImage));

            // Create an image for another user (should not be returned)
            AuthTestData otherUserData = authTestUtils.createAuthenticatedUserInSameAssociation(authData.association());
            Image otherUserImage = TestDataBuilder.image()
                    .user(otherUserData.user())
                    .association(authData.association())
                    .status(ImageStatus.PENDING)
                    .fileName("other-user-image.jpg")
                    .imageOrder(1)
                    .url("http://example.com/other-user.jpg")
                    .build();
            imagesRepository.save(otherUserImage);

            // Act
            ResultActions result = mockMvc.perform(get(baseEndpoint + "/images")
                    .with(user(authData.user().getEmail())));

            // Assert - Should only return pending images, not active images
            result.andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Images retrieved successfully"))
                    .andExpect(jsonPath("$.data.images").isArray())
                    .andExpect(jsonPath("$.data.images", hasSize(1)))
                    .andExpect(jsonPath("$.data.images[0].fileName").value("pending-image.jpg"));
        }

        @Test
        @DisplayName("Should not return images marked for deletion")
        void shouldNotReturnImagesMarkedForDeletion() throws Exception {
            // Arrange - Create pending and soft deleted images for the user
            Image pendingImage = TestDataBuilder.image()
                    .user(authData.user())
                    .association(authData.association())
                    .status(ImageStatus.PENDING)
                    .fileName("pending-image.jpg")
                    .imageOrder(1)
                    .url("http://example.com/pending.jpg")
                    .build();
            Image deletedImage = TestDataBuilder.image()
                    .user(authData.user())
                    .association(authData.association())
                    .status(ImageStatus.MARKED_FOR_DELETION)
                    .fileName("deleted-image.jpg")
                    .imageOrder(2)
                    .url("http://example.com/deleted.jpg")
                    .build();
            imagesRepository.saveAll(List.of(pendingImage, deletedImage));

            // Act
            ResultActions result = mockMvc.perform(get(baseEndpoint + "/images")
                    .with(user(authData.user().getEmail())));

            // Assert - Should only return pending image, not the soft deleted one
            result.andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.images", hasSize(1)))
                    .andExpect(jsonPath("$.data.images[0].fileName").value("pending-image.jpg"));
        }

        @Test
        @DisplayName("Should return empty array when user has no images")
        void shouldReturnEmptyArrayWhenUserHasNoImages() throws Exception {
            // Arrange - No images created for this user

            // Act
            ResultActions result = mockMvc.perform(get(baseEndpoint + "/images")
                    .with(user(authData.user().getEmail())));

            // Assert
            result.andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Images retrieved successfully"))
                    .andExpect(jsonPath("$.data.images").isArray())
                    .andExpect(jsonPath("$.data.images", hasSize(0)));
        }

        @Test
        @DisplayName("Should return 401 when user is not authenticated")
        void shouldReturn401WhenNotAuthenticated() throws Exception {
            // Act
            ResultActions result = mockMvc.perform(get(baseEndpoint + "/images"));

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
            ResultActions result = mockMvc.perform(get(baseEndpoint + "/images")
                    .with(user(otherUserData.user().getEmail())));

            // Assert
            result.andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Should return 403 when COLLABORATOR tries to get images")
        void shouldReturn403WhenCollaboratorTriesToGetImages() throws Exception {
            // Arrange
            AuthTestData collaboratorData = authTestUtils.createAuthenticatedUserInSameAssociation(
                    authData.association(), AssociationRole.COLLABORATOR);

            // Act
            ResultActions result = mockMvc.perform(get(baseEndpoint + "/images")
                    .with(user(collaboratorData.user().getEmail())));

            // Assert
            result.andExpect(status().isForbidden())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("Only administrators and members can delete images"));
        }

        @Test
        @DisplayName("Should successfully get images for ADMIN role")
        void shouldSuccessfullyGetImagesForAdmin() throws Exception {
            // Arrange
            AuthTestData adminData = authTestUtils.createAuthenticatedUserInSameAssociation(
                    authData.association(), AssociationRole.ADMIN);
            Image adminImage = TestDataBuilder.image()
                    .user(adminData.user())
                    .association(authData.association())
                    .status(ImageStatus.PENDING)
                    .fileName("admin-pending-image.jpg")
                    .imageOrder(1)
                    .url("http://example.com/admin-pending.jpg")
                    .build();
            imagesRepository.save(adminImage);

            String adminEndpoint = "/v1/associations/" + authData.association().getId() + "/images";

            // Act
            ResultActions result = mockMvc.perform(get(adminEndpoint + "/images")
                    .with(user(adminData.user().getEmail())));

            // Assert
            result.andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Images retrieved successfully"))
                    .andExpect(jsonPath("$.data.images", hasSize(1)))
                    .andExpect(jsonPath("$.data.images[0].fileName").value("admin-pending-image.jpg"));
        }

        @Test
        @DisplayName("Should successfully get images for MEMBER role")
        void shouldSuccessfullyGetImagesForMember() throws Exception {
            // Arrange
            AuthTestData memberData = authTestUtils.createAuthenticatedUserInSameAssociation(
                    authData.association(), AssociationRole.MEMBER);
            Image memberImage = TestDataBuilder.image()
                    .user(memberData.user())
                    .association(authData.association())
                    .status(ImageStatus.PENDING)
                    .fileName("member-pending-image.jpg")
                    .imageOrder(1)
                    .url("http://example.com/member-pending.jpg")
                    .build();
            imagesRepository.save(memberImage);

            String memberEndpoint = "/v1/associations/" + authData.association().getId() + "/images";

            // Act
            ResultActions result = mockMvc.perform(get(memberEndpoint + "/images")
                    .with(user(memberData.user().getEmail())));

            // Assert
            result.andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Images retrieved successfully"))
                    .andExpect(jsonPath("$.data.images", hasSize(1)))
                    .andExpect(jsonPath("$.data.images[0].fileName").value("member-pending-image.jpg"));
        }

        @Test
        @DisplayName("Should only return pending images, not active or soft deleted images")
        void shouldOnlyReturnPendingImagesNotActiveOrSoftDeleted() throws Exception {
            // Arrange - Create active, pending, and soft deleted images for the user
            Image activeImage = TestDataBuilder.image()
                    .user(authData.user())
                    .association(authData.association())
                    .status(ImageStatus.ACTIVE)
                    .fileName("active-image.jpg")
                    .imageOrder(1)
                    .url("http://example.com/active.jpg")
                    .build();
            Image pendingImage = TestDataBuilder.image()
                    .user(authData.user())
                    .association(authData.association())
                    .status(ImageStatus.PENDING)
                    .fileName("pending-image.jpg")
                    .imageOrder(2)
                    .url("http://example.com/pending.jpg")
                    .build();
            Image deletedImage = TestDataBuilder.image()
                    .user(authData.user())
                    .association(authData.association())
                    .status(ImageStatus.MARKED_FOR_DELETION)
                    .fileName("deleted-image.jpg")
                    .imageOrder(3)
                    .url("http://example.com/deleted.jpg")
                    .build();
            imagesRepository.saveAll(List.of(activeImage, pendingImage, deletedImage));

            // Act
            ResultActions result = mockMvc.perform(get(baseEndpoint + "/images")
                    .with(user(authData.user().getEmail())));

            // Assert - Should return only pending images, filtering out active and soft deleted ones
            result.andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.images", hasSize(1)))
                    .andExpect(jsonPath("$.data.images[0].fileName").value("pending-image.jpg"));
        }

        @Test
        @DisplayName("Should only return pending images without raffle associations")
        void shouldOnlyReturnPendingImagesWithoutRaffleAssociations() throws Exception {
            // Arrange - Create images with and without raffle associations
            Image pendingImageNoRaffle = TestDataBuilder.image()
                    .user(authData.user())
                    .association(authData.association())
                    .raffle(null) // No raffle association
                    .status(ImageStatus.PENDING)
                    .fileName("pending-no-raffle.jpg")
                    .imageOrder(1)
                    .url("http://example.com/pending-no-raffle.jpg")
                    .build();

            // Create a raffle and an image associated with it
            Raffle testRaffle = TestDataBuilder.raffle()
                    .association(authData.association())
                    .status(RaffleStatus.PENDING)
                    .title("Test Raffle")
                    .build();
            testRaffle = rafflesRepository.save(testRaffle);

            Image activeImageWithRaffle = TestDataBuilder.image()
                    .user(authData.user())
                    .association(authData.association())
                    .raffle(testRaffle)
                    .status(ImageStatus.ACTIVE)
                    .fileName("active-with-raffle.jpg")
                    .imageOrder(1)
                    .url("http://example.com/active-with-raffle.jpg")
                    .build();

            imagesRepository.saveAll(List.of(pendingImageNoRaffle, activeImageWithRaffle));

            // Act
            ResultActions result = mockMvc.perform(get(baseEndpoint + "/images")
                    .with(user(authData.user().getEmail())));

            // Assert - Should return only pending images without raffle associations
            result.andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.images", hasSize(1)))
                    .andExpect(jsonPath("$.data.images[0].fileName").value("pending-no-raffle.jpg"));
        }

        @Test
        @DisplayName("Should return only user's own images, not images from other users")
        void shouldReturnOnlyUserOwnImages() throws Exception {
            // Arrange - Create another user in the same association
            AuthTestData anotherUserData = authTestUtils.createAuthenticatedUserInSameAssociation(authData.association());

            // Create images for both users in the same association
            Image firstUserImage = TestDataBuilder.image()
                    .user(authData.user())
                    .association(authData.association())
                    .status(ImageStatus.PENDING)
                    .fileName("first-user-image.jpg")
                    .imageOrder(1)
                    .url("http://example.com/first-user.jpg")
                    .build();
            Image secondUserImage = TestDataBuilder.image()
                    .user(anotherUserData.user())
                    .association(authData.association())
                    .status(ImageStatus.PENDING)
                    .fileName("second-user-image.jpg")
                    .imageOrder(1)
                    .url("http://example.com/second-user.jpg")
                    .build();
            imagesRepository.saveAll(List.of(firstUserImage, secondUserImage));

            // Act - Get images for first user
            ResultActions result = mockMvc.perform(get(baseEndpoint + "/images")
                    .with(user(authData.user().getEmail())));

            // Assert - Should return only first user's images, not second user's
            result.andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.images", hasSize(1)))
                    .andExpect(jsonPath("$.data.images[0].fileName").value("first-user-image.jpg"));
        }
    }

    private byte[] createTestImageContent() {
        // Create a simple test image content (mock JPEG header)
        return new byte[]{
                (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0,
                0x00, 0x10, 0x4A, 0x46, 0x49, 0x46, 0x00, 0x01,
                0x01, 0x01, 0x00, 0x48, 0x00, 0x48, 0x00, 0x00,
                (byte) 0xFF, (byte) 0xD9 // End of JPEG marker
        };
    }
} 