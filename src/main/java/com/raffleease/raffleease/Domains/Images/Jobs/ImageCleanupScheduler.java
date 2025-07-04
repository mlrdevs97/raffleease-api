package com.raffleease.raffleease.Domains.Images.Jobs;

import com.raffleease.raffleease.Domains.Images.Model.Image;
import com.raffleease.raffleease.Domains.Images.Repository.ImagesRepository;
import com.raffleease.raffleease.Domains.Images.Services.ImagesDeleteService;
import lombok.RequiredArgsConstructor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Scheduled service for cleaning up old pending images from the database.
 * 
 * This service works in coordination with FileCleanupServiceImpl:
 * - ImageCleanupScheduler (this): Handles database cleanup of old pending images (runs at configured cron)
 * - FileCleanupServiceImpl: Handles filesystem cleanup of temp files and orphaned files (runs 30 min later)
 * 
 * When this service deletes an Image entity, the ImageEntityListener automatically
 * deletes the associated file from storage.
 */
@RequiredArgsConstructor
@Service
@Slf4j
public class ImageCleanupScheduler {
    private final ImagesDeleteService imagesDeleteService;
    private final ImagesRepository imagesRepository;

    @Value("${spring.application.configs.cleanup.images_cleanup_cutoff_seconds}")
    private Long cutoffSeconds;

    /**
     * Cleans up old pending images that have no associated raffle.
     * File deletion is handled automatically by ImageEntityListener.
     */
    @Scheduled(cron = "${spring.application.configs.cron.images_cleanup}")
    public void cleanOrphanImages() {
        log.info("Starting scheduled cleanup of old pending images");
        
        LocalDateTime cutoff = LocalDateTime.now().minusSeconds(cutoffSeconds);
        List<Image> oldOrphanImages = imagesRepository.findAllByRaffleIsNullAndCreatedAtBefore(cutoff);
        
        if (oldOrphanImages.isEmpty()) {
            log.debug("No old pending images found for cleanup");
            return;
        }
        
        log.info("Found {} old pending images to clean up (older than {})", oldOrphanImages.size(), cutoff);
        imagesDeleteService.deleteAll(oldOrphanImages);
        log.info("Successfully cleaned up {} old pending images", oldOrphanImages.size());
    }
}
