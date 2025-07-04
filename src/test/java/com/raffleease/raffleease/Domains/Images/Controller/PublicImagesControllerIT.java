package com.raffleease.raffleease.Domains.Images.Controller;

import com.raffleease.raffleease.Base.AbstractIntegrationTest;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.ResultActions;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("Public Images Controller Integration Tests")
class PublicImagesControllerIT extends AbstractIntegrationTest {

    @Autowired
    private AuthTestUtils authTestUtils;

    @Autowired
    private ImagesRepository imagesRepository;

    @Autowired
    private RafflesRepository rafflesRepository;

    @MockitoBean
    private FileStorageService fileStorageService;

    private AuthTestData authData;

    @BeforeEach
    void setUp() {
        authData = authTestUtils.createAuthenticatedUser();
    }

    @Nested
    @DisplayName("GET /public/v1/associations/{associationId}/images/{id}")
    class GetPendingImagesTests {

        private String publicPendingEndpoint;

        @BeforeEach
        void setUpPublicEndpoint() {
            publicPendingEndpoint = "/v1/public/associations/" + authData.association().getId() + "/images";
        }

        @Test
        @DisplayName("Should successfully get pending image file from public endpoint")
        void shouldGetPendingImageFileFromPublicEndpoint() throws Exception {
            // Arrange
            Image testImage = TestDataBuilder.image()
                    .user(authData.user())
                    .association(authData.association())
                    .status(ImageStatus.PENDING)
                    .fileName("pending-image.jpg")
                    .filePath("/test/path/pending-image.jpg")
                    .build();
            testImage = imagesRepository.save(testImage);

            when(fileStorageService.load("/test/path/pending-image.jpg"))
                    .thenReturn(new org.springframework.core.io.ByteArrayResource("pending image content".getBytes()));

            // Act
            ResultActions result = mockMvc.perform(get(publicPendingEndpoint + "/" + testImage.getId()));

            // Assert
            result.andExpect(status().isOk())
                    .andExpect(content().bytes("pending image content".getBytes()));
        }

        @Test
        @DisplayName("Should return 404 when pending image doesn't exist")
        void shouldReturn404WhenPendingImageDoesntExist() throws Exception {
            // Act
            ResultActions result = mockMvc.perform(get(publicPendingEndpoint + "/99999"));

            // Assert
            result.andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Should return 404 when pending image is marked for deletion")
        void shouldReturn404WhenPendingImageIsMarkedForDeletion() throws Exception {
            // Arrange
            Image testImage = TestDataBuilder.image()
                    .user(authData.user())
                    .association(authData.association())
                    .status(ImageStatus.MARKED_FOR_DELETION)
                    .fileName("deleted-pending-image.jpg")
                    .build();
            testImage = imagesRepository.save(testImage);

            // Act
            ResultActions result = mockMvc.perform(get(publicPendingEndpoint + "/" + testImage.getId()));

            // Assert
            result.andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Should successfully get active image file from public pending endpoint")
        void shouldGetActiveImageFileFromPublicPendingEndpoint() throws Exception {
            // Arrange - Test that active images can also be retrieved through this endpoint
            Image testImage = TestDataBuilder.image()
                    .user(authData.user())
                    .association(authData.association())
                    .status(ImageStatus.ACTIVE)
                    .fileName("active-image.jpg")
                    .filePath("/test/path/active-image.jpg")
                    .build();
            testImage = imagesRepository.save(testImage);

            when(fileStorageService.load("/test/path/active-image.jpg"))
                    .thenReturn(new org.springframework.core.io.ByteArrayResource("active image content".getBytes()));

            // Act
            ResultActions result = mockMvc.perform(get(publicPendingEndpoint + "/" + testImage.getId()));

            // Assert
            result.andExpect(status().isOk())
                    .andExpect(content().bytes("active image content".getBytes()));
        }

        @Test
        @DisplayName("Should allow access to pending images from any association without authentication")
        void shouldAllowAccessToPendingImagesFromAnyAssociationWithoutAuthentication() throws Exception {
            // Arrange - Create another association and image
            AuthTestData otherUserData = authTestUtils.createAuthenticatedUserWithCredentials(
                    "otheruser", "other@example.com", "password123");
            
            Image otherAssociationImage = TestDataBuilder.image()
                    .user(otherUserData.user())
                    .association(otherUserData.association())
                    .status(ImageStatus.PENDING)
                    .fileName("other-association-pending-image.jpg")
                    .filePath("/test/path/other-association-pending-image.jpg")
                    .build();
            otherAssociationImage = imagesRepository.save(otherAssociationImage);

            when(fileStorageService.load("/test/path/other-association-pending-image.jpg"))
                    .thenReturn(new org.springframework.core.io.ByteArrayResource("other association pending image content".getBytes()));

            String otherPublicPendingEndpoint = "/v1/public/associations/" + otherUserData.association().getId() + "/images";

            // Act - Access without any authentication
            ResultActions result = mockMvc.perform(get(otherPublicPendingEndpoint + "/" + otherAssociationImage.getId()));

            // Assert - Should succeed since it's public
            result.andExpect(status().isOk())
                    .andExpect(content().bytes("other association pending image content".getBytes()));
        }
    }

    @Nested
    @DisplayName("GET /public/v1/associations/{associationId}/raffles/{raffleId}/images/{id}")
    class GetRaffleImagesTests {

        private Raffle testRaffle;
        private String publicRaffleEndpoint;

        @BeforeEach
        void setUpRaffleAndEndpoint() {
            // Create a test raffle
            testRaffle = TestDataBuilder.raffle()
                    .association(authData.association())
                    .status(RaffleStatus.PENDING)
                    .title("Test Raffle for Public Images")
                    .build();
            testRaffle = rafflesRepository.save(testRaffle);
            
            publicRaffleEndpoint = "/v1/public/associations/" + authData.association().getId() + "/raffles/" + testRaffle.getId() + "/images";
        }

        @Test
        @DisplayName("Should successfully get raffle image file from public endpoint")
        void shouldGetRaffleImageFileFromPublicEndpoint() throws Exception {
            // Arrange
            Image testImage = TestDataBuilder.image()
                    .user(authData.user())
                    .association(authData.association())
                    .raffle(testRaffle)
                    .status(ImageStatus.ACTIVE)
                    .fileName("test-image.jpg")
                    .filePath("/test/path/test-image.jpg")
                    .build();
            testImage = imagesRepository.save(testImage);

            when(fileStorageService.load("/test/path/test-image.jpg"))
                    .thenReturn(new org.springframework.core.io.ByteArrayResource("test image content".getBytes()));

            // Act
            ResultActions result = mockMvc.perform(get(publicRaffleEndpoint + "/" + testImage.getId()));

            // Assert
            result.andExpect(status().isOk())
                    .andExpect(content().bytes("test image content".getBytes()));
        }

        @Test
        @DisplayName("Should return 404 when raffle image doesn't exist")
        void shouldReturn404WhenRaffleImageDoesntExist() throws Exception {
            // Act
            ResultActions result = mockMvc.perform(get(publicRaffleEndpoint + "/99999"));

            // Assert
            result.andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Should return 404 when raffle image is marked for deletion")
        void shouldReturn404WhenRaffleImageIsMarkedForDeletion() throws Exception {
            // Arrange
            Image testImage = TestDataBuilder.image()
                    .user(authData.user())
                    .association(authData.association())
                    .raffle(testRaffle)
                    .status(ImageStatus.MARKED_FOR_DELETION)
                    .fileName("deleted-image.jpg")
                    .build();
            testImage = imagesRepository.save(testImage);

            // Act
            ResultActions result = mockMvc.perform(get(publicRaffleEndpoint + "/" + testImage.getId()));

            // Assert
            result.andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Should successfully get pending raffle image file from public endpoint")
        void shouldGetPendingRaffleImageFileFromPublicEndpoint() throws Exception {
            // Arrange
            Image testImage = TestDataBuilder.image()
                    .user(authData.user())
                    .association(authData.association())
                    .raffle(testRaffle)
                    .status(ImageStatus.PENDING)
                    .fileName("pending-image.jpg")
                    .filePath("/test/path/pending-image.jpg")
                    .build();
            testImage = imagesRepository.save(testImage);

            when(fileStorageService.load("/test/path/pending-image.jpg"))
                    .thenReturn(new org.springframework.core.io.ByteArrayResource("pending image content".getBytes()));

            // Act
            ResultActions result = mockMvc.perform(get(publicRaffleEndpoint + "/" + testImage.getId()));

            // Assert
            result.andExpect(status().isOk())
                    .andExpect(content().bytes("pending image content".getBytes()));
        }

        @Test
        @DisplayName("Should allow access to raffle images from any association without authentication")
        void shouldAllowAccessToRaffleImagesFromAnyAssociationWithoutAuthentication() throws Exception {
            // Arrange - Create another association and image
            AuthTestData otherUserData = authTestUtils.createAuthenticatedUserWithCredentials(
                    "otheruser", "other@example.com", "password123");
            
            // Create a raffle for the other association
            Raffle otherRaffle = TestDataBuilder.raffle()
                    .association(otherUserData.association())
                    .status(RaffleStatus.PENDING)
                    .title("Other Association Raffle")
                    .build();
            otherRaffle = rafflesRepository.save(otherRaffle);
            
            Image otherAssociationImage = TestDataBuilder.image()
                    .user(otherUserData.user())
                    .association(otherUserData.association())
                    .raffle(otherRaffle)
                    .status(ImageStatus.ACTIVE)
                    .fileName("other-association-image.jpg")
                    .filePath("/test/path/other-association-image.jpg")
                    .build();
            otherAssociationImage = imagesRepository.save(otherAssociationImage);

            when(fileStorageService.load("/test/path/other-association-image.jpg"))
                    .thenReturn(new org.springframework.core.io.ByteArrayResource("other association image content".getBytes()));

            String otherPublicRaffleEndpoint = "/v1/public/associations/" + otherUserData.association().getId() +
                                           "/raffles/" + otherRaffle.getId() + "/images";

            // Act - Access without any authentication
            ResultActions result = mockMvc.perform(get(otherPublicRaffleEndpoint + "/" + otherAssociationImage.getId()));

            // Assert - Should succeed since it's public
            result.andExpect(status().isOk())
                    .andExpect(content().bytes("other association image content".getBytes()));
        }

        @Test
        @DisplayName("Should work regardless of which public endpoint is used for same image")
        void shouldWorkRegardlessOfWhichPublicEndpointIsUsedForSameImage() throws Exception {
            // Arrange - Create an image that could be accessed through either endpoint
            Image testImage = TestDataBuilder.image()
                    .user(authData.user())
                    .association(authData.association())
                    .raffle(testRaffle)
                    .status(ImageStatus.ACTIVE)
                    .fileName("shared-image.jpg")
                    .filePath("/test/path/shared-image.jpg")
                    .build();
            testImage = imagesRepository.save(testImage);

            when(fileStorageService.load("/test/path/shared-image.jpg"))
                    .thenReturn(new org.springframework.core.io.ByteArrayResource("shared image content".getBytes()));

            String pendingEndpoint = "/v1/public/associations/" + authData.association().getId() + "/images";
            String raffleEndpoint = "/v1/public/associations/" + authData.association().getId() + "/raffles/" + testRaffle.getId() + "/images";

            // Act & Assert - Both endpoints should work for the same image
            mockMvc.perform(get(pendingEndpoint + "/" + testImage.getId()))
                    .andExpect(status().isOk())
                    .andExpect(content().bytes("shared image content".getBytes()));

            mockMvc.perform(get(raffleEndpoint + "/" + testImage.getId()))
                    .andExpect(status().isOk())
                    .andExpect(content().bytes("shared image content".getBytes()));
        }
    }
} 