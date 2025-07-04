package com.raffleease.raffleease.Domains.Images.Services;

import com.raffleease.raffleease.Domains.Images.Model.Image;

import java.util.List;

public interface ImagesDeleteService {
    /**
     * Deletes multiple images at once.
     * Used by cleanup scheduler.
     * 
     * @param images the images to delete
     */
    void deleteAll(List<Image> images);
    
    /**
     * Deletes a single image.
     * Used internally for cleanup operations.
     * 
     * @param image the image to delete
     */
    void delete(Image image);

    /**
     * Soft deletes an image by setting the status to MARKED_FOR_DELETION.
     * Soft deleted images cannot be fetched by the API and will be deleted when raffle create/edit process completes.
     * 
     * @param id the ID of the image to soft delete
     */
    void softDelete(Long id);
}
