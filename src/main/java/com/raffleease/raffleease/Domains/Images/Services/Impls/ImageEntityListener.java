package com.raffleease.raffleease.Domains.Images.Services.Impls;

import com.raffleease.raffleease.Domains.Images.Model.Image;
import com.raffleease.raffleease.Domains.Images.Services.FileStorageService;
import jakarta.persistence.PreRemove;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * This listener ensures automatic file cleanup when images are deleted via:
 * - ImageCleanupScheduler: Deletes old pending images from database
 * - Manual deletion through ImagesDeleteService
 * - Cascade deletions when parent entities are removed
 */
@RequiredArgsConstructor
@Component
@Slf4j
public class ImageEntityListener {
    private final FileStorageService fileStorageService;

    /**
     * Automatically deletes the associated file when an Image entity is being removed.
     * This ensures no orphaned files are left on the filesystem.
     */
    @PreRemove
    public void onPreRemove(Image image) {
        if (image.getFilePath() != null) {
            log.debug("Deleting file for removed image entity: {}", image.getFilePath());
            fileStorageService.delete(image.getFilePath());
        }
    }
}
