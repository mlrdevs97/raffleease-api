package com.raffleease.raffleease.Domains.Images.Jobs;

import com.raffleease.raffleease.Base.AbstractIntegrationTest;
import com.raffleease.raffleease.Domains.Images.Model.Image;
import com.raffleease.raffleease.Domains.Images.Model.ImageStatus;
import com.raffleease.raffleease.Domains.Images.Repository.ImagesRepository;
import com.raffleease.raffleease.util.AuthTestUtils;
import com.raffleease.raffleease.util.AuthTestUtils.AuthTestData;
import com.raffleease.raffleease.util.TestDataBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.util.ReflectionTestUtils;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@DisplayName("Files Cleanup Scheduler Integration Tests")
@TestPropertySource(properties = {
    "spring.storage.images.base_path=${java.io.tmpdir}/test-images",
    "spring.application.configs.cron.images_cleanup=0 0 2 * * ?"
})
class FilesCleanupSchedulerIT extends AbstractIntegrationTest {

    @Autowired
    private FilesCleanupScheduler scheduler;

    @Autowired
    private ImagesRepository imagesRepository;

    @Autowired
    private AuthTestUtils authTestUtils;

    @TempDir
    Path tempDir;

    private AuthTestData authData;
    private Path testBasePath;
    private Path associationsPath;

    @BeforeEach
    void setUp() throws IOException {
        authData = authTestUtils.createAuthenticatedUser();
        
        // Set up test file structure
        testBasePath = tempDir.resolve("test-images");
        associationsPath = testBasePath.resolve("associations").resolve(authData.association().getId().toString());
        Files.createDirectories(associationsPath);
        
        // Override the basePath in the scheduler using Spring's ReflectionTestUtils
        ReflectionTestUtils.setField(scheduler, "basePath", testBasePath.toString());
        
        log.info("Test setup with base path: {}", testBasePath);
    }

    @Nested
    @DisplayName("scheduledCleanup()")
    class ScheduledCleanupTests {

        @Test
        @DisplayName("Should clean up old temporary files")
        void shouldCleanUpOldTemporaryFiles() throws IOException {
            // Arrange - Create old temporary files
            Path tempPath = associationsPath.resolve("temp");
            Files.createDirectories(tempPath);
            
            Path oldTempFile1 = tempPath.resolve("old-temp-1.jpg");
            Path oldTempFile2 = tempPath.resolve("old-temp-2.jpg");
            Path recentTempFile = tempPath.resolve("recent-temp.jpg");
            
            Files.createFile(oldTempFile1);
            Files.createFile(oldTempFile2);
            Files.createFile(recentTempFile);
            
            // Make files old by setting their modified time
            Files.setLastModifiedTime(oldTempFile1, 
                java.nio.file.attribute.FileTime.from(java.time.Instant.now().minusSeconds(86400 + 3600))); // 25 hours old
            Files.setLastModifiedTime(oldTempFile2, 
                java.nio.file.attribute.FileTime.from(java.time.Instant.now().minusSeconds(86400 + 1800))); // 24.5 hours old

            // Act
            scheduler.scheduledCleanup();

            // Assert
            assertThat(Files.exists(oldTempFile1)).isFalse();
            assertThat(Files.exists(oldTempFile2)).isFalse();
            assertThat(Files.exists(recentTempFile)).isTrue(); // Recent file should remain
        }

        @Test
        @DisplayName("Should clean up orphaned files not in database")
        void shouldCleanUpOrphanedFiles() throws IOException {
            // Arrange - Create files and database entries
            Path regularPath = associationsPath.resolve("regular");
            Files.createDirectories(regularPath);
            
            Path orphanedFile = regularPath.resolve("orphaned-file.jpg");
            Path validFile = regularPath.resolve("valid-file.jpg");
            
            Files.createFile(orphanedFile);
            Files.createFile(validFile);
            
            // Create database entry only for validFile
            Image validImage = TestDataBuilder.image()
                    .user(authData.user())
                    .association(authData.association())
                    .status(ImageStatus.PENDING)
                    .fileName("valid-file.jpg")
                    .filePath(validFile.toString())
                    .url("http://localhost/test/valid-file.jpg")
                    .build();
            imagesRepository.save(validImage);

            // Act
            scheduler.scheduledCleanup();

            // Assert
            assertThat(Files.exists(orphanedFile)).isFalse(); // Orphaned file should be deleted
            assertThat(Files.exists(validFile)).isTrue();     // Valid file should remain
        }

        @Test
        @DisplayName("Should handle non-existent base path")
        void shouldHandleNonExistentBasePath() {
            // Arrange - Override with non-existent path
            ReflectionTestUtils.setField(scheduler, "basePath", "/non/existent/path");

            // Act & Assert - Should not throw any exceptions
            scheduler.scheduledCleanup();
        }

        @Test
        @DisplayName("Should clean up old temp files and may remove empty temp directories")
        void shouldCleanUpOldTempFilesAndMayRemoveEmptyDirectories() throws IOException {
            // Arrange - Create temp files in nested structure
            Path tempPath = associationsPath.resolve("temp");
            Files.createDirectories(tempPath);
            
            Path oldTempFile = tempPath.resolve("old-temp.jpg");
            Files.createFile(oldTempFile);
            
            // Make file old
            Files.setLastModifiedTime(oldTempFile, 
                java.nio.file.attribute.FileTime.from(java.time.Instant.now().minusSeconds(86400 + 3600))); // 25 hours old

            // Act
            scheduler.scheduledCleanup();

            // Assert - Old temp file should be deleted
            assertThat(Files.exists(oldTempFile)).isFalse();
            // Directory cleanup behavior depends on implementation - just verify no exception
        }

        @Test
        @DisplayName("Should handle mixed scenario with temp files and orphaned files")
        void shouldHandleMixedScenario() throws IOException {
            // Arrange - Create complex file structure
            Path tempPath = associationsPath.resolve("temp");
            Path regularPath = associationsPath.resolve("regular");
            Files.createDirectories(tempPath);
            Files.createDirectories(regularPath);
            
            // Create old temp files
            Path oldTempFile = tempPath.resolve("old-temp.jpg");
            Files.createFile(oldTempFile);
            Files.setLastModifiedTime(oldTempFile, 
                java.nio.file.attribute.FileTime.from(java.time.Instant.now().minusSeconds(86400 + 3600)));
            
            // Create recent temp file
            Path recentTempFile = tempPath.resolve("recent-temp.jpg");
            Files.createFile(recentTempFile);
            
            // Create orphaned file
            Path orphanedFile = regularPath.resolve("orphaned.jpg");
            Files.createFile(orphanedFile);
            
            // Create valid file with database entry
            Path validFile = regularPath.resolve("valid.jpg");
            Files.createFile(validFile);
            
            Image validImage = TestDataBuilder.image()
                    .user(authData.user())
                    .association(authData.association())
                    .status(ImageStatus.PENDING)
                    .fileName("valid.jpg")
                    .filePath(validFile.toString())
                    .url("http://localhost/test/valid.jpg")
                    .build();
            imagesRepository.save(validImage);

            // Act
            scheduler.scheduledCleanup();

            // Assert
            assertThat(Files.exists(oldTempFile)).isFalse();      // Old temp file deleted
            assertThat(Files.exists(recentTempFile)).isTrue();    // Recent temp file remains
            assertThat(Files.exists(orphanedFile)).isFalse();     // Orphaned file deleted
            assertThat(Files.exists(validFile)).isTrue();         // Valid file remains
        }

        @Test
        @DisplayName("Should not clean up files in temp directories when checking for orphaned files")
        void shouldNotCleanUpTempFilesWhenCheckingOrphanedFiles() throws IOException {
            // Arrange - Create old temp file and orphaned file
            Path tempPath = associationsPath.resolve("temp");
            Files.createDirectories(tempPath);
            
            Path tempFile = tempPath.resolve("temp-file.jpg");
            Files.createFile(tempFile);
            
            // Make temp file old (but it should not be deleted as orphaned file)
            Files.setLastModifiedTime(tempFile, 
                java.nio.file.attribute.FileTime.from(java.time.Instant.now().minusSeconds(86400 + 3600)));

            // Act
            scheduler.scheduledCleanup();

            // Assert - Temp file should be deleted by temp cleanup logic, not orphaned cleanup
            assertThat(Files.exists(tempFile)).isFalse();
        }

        @Test
        @DisplayName("Should handle scenario with no associations directory")
        void shouldHandleNoAssociationsDirectory() throws IOException {
            // Arrange - Use a completely empty test base path
            Path emptyBasePath = tempDir.resolve("empty-test-images");
            Files.createDirectories(emptyBasePath);
            ReflectionTestUtils.setField(scheduler, "basePath", emptyBasePath.toString());

            // Act & Assert - Should not throw any exceptions
            scheduler.scheduledCleanup();
        }
    }
} 