package com.raffleease.raffleease.Domains.Images.Jobs;

import com.raffleease.raffleease.Domains.Images.Repository.ImagesRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Scheduled service responsible for filesystem cleanup operations.
 * 
 * This service handles:
 * - Cleanup of old temporary files
 * - Cleanup of orphaned files (files that exist on disk but have no database entry)
 * 
 * Note: Database cleanup is handled separately by ImageCleanupScheduler.
 */
@RequiredArgsConstructor
@Service
@Slf4j
public class FilesCleanupScheduler {
    
    private final ImagesRepository imagesRepository;
    
    @Value("${spring.storage.images.base_path}")
    private String basePath;

    @Scheduled(cron = "${spring.application.configs.cron.images_cleanup}")
    public void scheduledCleanup() {
        log.info("Starting scheduled filesystem cleanup");
        try {
            performFullCleanup();
        } catch (Exception e) {
            log.error("Error during scheduled filesystem cleanup", e);
        }
    }

    /**
     * Cleans up temporary files older than the specified duration
     * @param olderThan Duration after which temporary files should be cleaned up
     * @return Number of files cleaned up
     */
    private int cleanupOldTemporaryFiles(Duration olderThan) {
        log.info("Starting cleanup of temporary files older than {}", olderThan);
        int cleanedUp = 0;
        
        try {
            Path tempBasePath = Paths.get(basePath, "associations");
            if (!Files.exists(tempBasePath)) {
                log.debug("Temporary base path doesn't exist: {}", tempBasePath);
                return 0;
            }
            
            cleanedUp = cleanupOldFilesInDirectory(tempBasePath, "temp", olderThan);
            log.info("Cleaned up {} old temporary files", cleanedUp);
            
        } catch (IOException e) {
            log.error("Error during temporary files cleanup", e);
        }
        
        return cleanedUp;
    }

    /**
     * Cleans up orphaned files that exist on disk but have no corresponding database entry
     * @return Number of orphaned files cleaned up
     */
    private int cleanupOrphanedFiles() {
        log.info("Starting cleanup of orphaned files from filesystem");
        int cleanedUp = 0;
        
        try {
            // Clean up orphaned files that exist on filesystem but have no corresponding database entry
            int filesCleanedUp = cleanupOrphanedFilesFromFilesystem();
            log.info("Cleaned up {} orphaned files from filesystem", filesCleanedUp);
            
            cleanedUp = filesCleanedUp;
            
        } catch (Exception e) {
            log.error("Error during orphaned files cleanup", e);
        }
        
        return cleanedUp;
    }

    /**
     * Performs a full cleanup including both old temporary files and orphaned files
     * @return Total number of files cleaned up
     */
    private int performFullCleanup() {
        log.info("Starting full filesystem cleanup");
        
        int tempFilesCleanedUp = cleanupOldTemporaryFiles(Duration.ofHours(24));
        int orphanedFilesCleanedUp = cleanupOrphanedFiles();
        
        int totalCleanedUp = tempFilesCleanedUp + orphanedFilesCleanedUp;
        log.info("Full filesystem cleanup completed. Total files cleaned up: {}", totalCleanedUp);
        
        return totalCleanedUp;
    }

    /**
     * Cleans up orphaned files that exist on filesystem but have no corresponding database entry
     */
    private int cleanupOrphanedFilesFromFilesystem() throws IOException {
        Set<String> dbFilePaths = imagesRepository.findAllFilePaths()
                .stream()
                .filter(path -> path != null && !path.isEmpty())
                .collect(Collectors.toSet());
        
        log.debug("Found {} file paths in database", dbFilePaths.size());
        
        Path associationsPath = Paths.get(basePath, "associations");
        if (!Files.exists(associationsPath)) {
            log.debug("Associations path doesn't exist: {}", associationsPath);
            return 0;
        }
        
        return findAndCleanupOrphanedFiles(associationsPath, dbFilePaths);
    }

    private int cleanupOldFilesInDirectory(Path basePath, String targetDirectoryName, Duration olderThan) throws IOException {
        int cleanedUp = 0;
        Instant cutoffTime = Instant.now().minus(olderThan);
        
        List<Path> filesToDelete = new ArrayList<>();
        List<Path> directoriesToDelete = new ArrayList<>();
        
        Files.walk(basePath)
                .filter(path -> Files.isDirectory(path) && path.getFileName().toString().equals(targetDirectoryName))
                .forEach(tempDir -> {
                    try {
                        Files.walk(tempDir)
                                .filter(Files::isRegularFile)
                                .forEach(file -> {
                                    try {
                                        BasicFileAttributes attrs = Files.readAttributes(file, BasicFileAttributes.class);
                                        if (attrs.lastModifiedTime().toInstant().isBefore(cutoffTime)) {
                                            filesToDelete.add(file);
                                        }
                                    } catch (IOException e) {
                                        log.warn("Could not read attributes for file: {}", file, e);
                                    }
                                });
                        
                        boolean willBeEmpty = Files.list(tempDir)
                                .allMatch(path -> filesToDelete.contains(path) || Files.isDirectory(path));
                        
                        if (willBeEmpty) {
                            directoriesToDelete.add(tempDir);
                        }
                        
                    } catch (IOException e) {
                        log.warn("Could not process temp directory: {}", tempDir, e);
                    }
                });
        
        for (Path file : filesToDelete) {
            try {
                Files.deleteIfExists(file);
                cleanedUp++;
                log.debug("Deleted old temporary file: {}", file);
            } catch (IOException e) {
                log.warn("Could not delete old temporary file: {}", file, e);
            }
        }
        
        for (Path dir : directoriesToDelete) {
            try {
                if (isDirectoryEmpty(dir)) {
                    Files.deleteIfExists(dir);
                    log.debug("Deleted empty temporary directory: {}", dir);
                }
            } catch (IOException e) {
                log.warn("Could not delete empty temporary directory: {}", dir, e);
            }
        }
        
        return cleanedUp;
    }

    private int findAndCleanupOrphanedFiles(Path basePath, Set<String> dbFilePaths) throws IOException {
        int cleanedUp = 0;
        
        List<Path> actualFiles = Files.walk(basePath)
                .filter(Files::isRegularFile)
                .filter(path -> !path.toString().contains("/temp/"))
                .collect(Collectors.toList());
        
        for (Path actualFile : actualFiles) {
            String absolutePath = actualFile.toString();
            
            if (!dbFilePaths.contains(absolutePath)) {
                try {
                    Files.deleteIfExists(actualFile);
                    cleanedUp++;
                    log.debug("Deleted orphaned file: {}", actualFile);
                } catch (IOException e) {
                    log.warn("Could not delete orphaned file: {}", actualFile, e);
                }
            }
        }
        
        return cleanedUp;
    }

    private boolean isDirectoryEmpty(Path directory) {
        try {
            return Files.list(directory).findAny().isEmpty();
        } catch (IOException e) {
            return false;
        }
    }
}
