package com.raffleease.raffleease.Domains.Images.Jobs;

import com.raffleease.raffleease.Base.AbstractIntegrationTest;
import com.raffleease.raffleease.Domains.Images.Model.Image;
import com.raffleease.raffleease.Domains.Images.Model.ImageStatus;
import com.raffleease.raffleease.Domains.Images.Repository.ImagesRepository;
import com.raffleease.raffleease.Domains.Raffles.Repository.RafflesRepository;
import com.raffleease.raffleease.util.AuthTestUtils;
import com.raffleease.raffleease.util.AuthTestUtils.AuthTestData;
import com.raffleease.raffleease.util.TestDataBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import lombok.extern.slf4j.Slf4j;

import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@DisplayName("Image Cleanup Scheduler Integration Tests")
@TestPropertySource(properties = {
    "spring.application.configs.cleanup.images_cleanup_cutoff_seconds=300"
})
class ImageCleanupSchedulerIT extends AbstractIntegrationTest {

    @Autowired
    private ImageCleanupScheduler scheduler;

    @Autowired
    private ImagesRepository imagesRepository;

    @Autowired
    private RafflesRepository rafflesRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private AuthTestUtils authTestUtils;

    private AuthTestData authData;
    private List<Image> testImages;

    @BeforeEach
    void setUp() {
        authData = authTestUtils.createAuthenticatedUser();
        
        // Create test images with different creation dates
        LocalDateTime now = LocalDateTime.now();
        log.info("Test setup time: {}", now);
        
        testImages = List.of(
            createTestPendingImage("Old Pending Image 1", now.minusMinutes(10)),
            createTestPendingImage("Old Pending Image 2", now.minusMinutes(8)),
            createTestPendingImage("Recent Pending Image", now.minusMinutes(2)),
            createTestAssociatedImage("Associated Image", now.minusMinutes(10))
        );
    }

    @Transactional
    private Image createTestPendingImage(String fileName, LocalDateTime createdAt) {
        Image image = TestDataBuilder.image()
                .user(authData.user())
                .association(authData.association())
                .status(ImageStatus.PENDING)
                .fileName(fileName)
                .filePath("/test/path/" + fileName)
                .url("http://localhost/test/" + fileName)
                .build();
        
        // Save first to get the ID
        image = imagesRepository.save(image);
        
        // Update the createdAt timestamp directly in the database to bypass @CreationTimestamp
        entityManager.createQuery("UPDATE Image i SET i.createdAt = :createdAt WHERE i.id = :id")
                .setParameter("createdAt", createdAt)
                .setParameter("id", image.getId())
                .executeUpdate();
        
        entityManager.flush();
        entityManager.clear(); // Clear the persistence context to force reload
        
        return imagesRepository.findById(image.getId()).orElse(image);
    }

    @Transactional
    private Image createTestAssociatedImage(String fileName, LocalDateTime createdAt) {
        // Create a test raffle first
        var testRaffle = TestDataBuilder.raffle()
                .association(authData.association())
                .title("Test Raffle")
                .build();
        testRaffle = rafflesRepository.save(testRaffle);

        Image image = TestDataBuilder.image()
                .user(authData.user())
                .association(authData.association())
                .status(ImageStatus.ACTIVE)
                .fileName(fileName)
                .filePath("/test/path/" + fileName)
                .url("http://localhost/test/" + fileName)
                .raffle(testRaffle) // This makes it associated, not pending
                .build();
        
        // Save first to get the ID
        image = imagesRepository.save(image);
        
        // Update the createdAt timestamp directly in the database to bypass @CreationTimestamp
        entityManager.createQuery("UPDATE Image i SET i.createdAt = :createdAt WHERE i.id = :id")
                .setParameter("createdAt", createdAt)
                .setParameter("id", image.getId())
                .executeUpdate();
        
        entityManager.flush();
        entityManager.clear(); // Clear the persistence context to force reload
        
        return imagesRepository.findById(image.getId()).orElse(image);
    }

    @Nested
    @DisplayName("cleanOrphanImages()")
    class CleanOrphanImagesTests {

        @Test
        @DisplayName("Should clean up old pending images that exceed cutoff time")
        void shouldCleanUpOldPendingImages() {
            // Act
            scheduler.cleanOrphanImages();

            // Assert
            List<Image> remainingImages = imagesRepository.findAll();
            
            // Old pending images should be deleted
            assertThat(findImageByFileName(remainingImages, "Old Pending Image 1")).isNull();
            assertThat(findImageByFileName(remainingImages, "Old Pending Image 2")).isNull();
            
            // Recent pending image should remain (within cutoff)
            assertThat(findImageByFileName(remainingImages, "Recent Pending Image")).isNotNull();
            
            // Associated image should remain (not orphaned)
            assertThat(findImageByFileName(remainingImages, "Associated Image")).isNotNull();
        }

        @Test
        @DisplayName("Should not clean up pending images within cutoff time")
        void shouldNotCleanUpRecentPendingImages() {
            // Arrange - Create a very recent pending image
            LocalDateTime now = LocalDateTime.now();
            Image veryRecentImage = createTestPendingImage("Very Recent Image", now.minusSeconds(30));

            // Act
            scheduler.cleanOrphanImages();

            // Assert
            List<Image> remainingImages = imagesRepository.findAll();
            
            // Very recent image should remain
            assertThat(findImageByFileName(remainingImages, "Very Recent Image")).isNotNull();
        }

        @Test
        @DisplayName("Should not clean up images associated with raffles regardless of age")
        void shouldNotCleanUpAssociatedImages() {
            // Arrange - Create an old associated image
            LocalDateTime veryOldTime = LocalDateTime.now().minusHours(24);
            Image oldAssociatedImage = createTestAssociatedImage("Old Associated Image", veryOldTime);

            // Act
            scheduler.cleanOrphanImages();

            // Assert
            List<Image> remainingImages = imagesRepository.findAll();
            
            // Old associated image should remain
            assertThat(findImageByFileName(remainingImages, "Old Associated Image")).isNotNull();
        }

        @Test
        @DisplayName("Should handle empty list of orphan images gracefully")
        void shouldHandleEmptyListOfOrphanImages() {
            // Arrange - Delete all pending images, keeping only associated ones
            List<Image> pendingImages = imagesRepository.findAllByRaffleIsNullAndUserAndStatus(
                authData.user(), ImageStatus.PENDING);
            imagesRepository.deleteAll(pendingImages);

            // Act
            scheduler.cleanOrphanImages();

            // Assert - Should not throw any exceptions
            List<Image> remainingImages = imagesRepository.findAll();
            
            // Only associated images should remain
            long associatedImagesCount = remainingImages.stream()
                    .filter(img -> img.getRaffle() != null)
                    .count();
            assertThat(associatedImagesCount).isGreaterThan(0);
        }

        @Test
        @DisplayName("Should handle scenario with no images at all")
        void shouldHandleNoImagesScenario() {
            // Arrange - Delete all test images
            imagesRepository.deleteAll();

            // Act
            scheduler.cleanOrphanImages();

            // Assert - Should not throw any exceptions
            List<Image> remainingImages = imagesRepository.findAll();
            assertThat(remainingImages).isEmpty();
        }

        @Test
        @DisplayName("Should handle mixed scenarios with various image states")
        void shouldHandleMixedImageStates() {
            // Arrange - Create complex scenario
            LocalDateTime now = LocalDateTime.now();
            List<Image> mixedImages = List.of(
                createTestPendingImage("Old Pending 1", now.minusMinutes(15)),
                createTestPendingImage("Old Pending 2", now.minusMinutes(12)),
                createTestPendingImage("Recent Pending 1", now.minusMinutes(1)),
                createTestPendingImage("Recent Pending 2", now.minusSeconds(30)),
                createTestAssociatedImage("Old Associated", now.minusMinutes(20)),
                createTestAssociatedImage("Recent Associated", now.minusMinutes(1))
            );

            // Act
            scheduler.cleanOrphanImages();

            // Assert
            List<Image> remainingImages = imagesRepository.findAll();
            
            // Only recent pending and all associated images should remain
            assertThat(findImageByFileName(remainingImages, "Old Pending 1")).isNull();
            assertThat(findImageByFileName(remainingImages, "Old Pending 2")).isNull();
            assertThat(findImageByFileName(remainingImages, "Recent Pending 1")).isNotNull();
            assertThat(findImageByFileName(remainingImages, "Recent Pending 2")).isNotNull();
            assertThat(findImageByFileName(remainingImages, "Old Associated")).isNotNull();
            assertThat(findImageByFileName(remainingImages, "Recent Associated")).isNotNull();
        }
    }

    private Image findImageByFileName(List<Image> images, String fileName) {
        return images.stream()
                .filter(img -> img.getFileName().equals(fileName))
                .findFirst()
                .orElse(null);
    }
} 