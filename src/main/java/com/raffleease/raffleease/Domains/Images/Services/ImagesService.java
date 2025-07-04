package com.raffleease.raffleease.Domains.Images.Services;

import com.raffleease.raffleease.Domains.Images.DTOs.UserImagesResponse;
import com.raffleease.raffleease.Domains.Images.Model.Image;
import org.springframework.core.io.Resource;

import java.util.List;

public interface ImagesService {
    /**
     * Saves a list of images.
     * 
     * @param images the images to save
     * @return the saved images
     */
    List<Image> saveAll(List<Image> images);
    
    /**
     * Finds an image by its ID.
     * 
     * @param id the ID of the image
     * @return the image
     */
    Image findById(Long id);
    
    /**
     * Finds all images by their IDs.
     * 
     * @param ids the IDs of the images
     * @return the images
     */
    List<Image> findAllById(List<Long> ids);
    
    /**
     * Gets the file associated with an image by its ID.
     * 
     * @param id the ID of the file
     * @return the file
     */
    Resource getFile(Long id);

    /*
     * Fetches all images for a user. 
     * Useful during the creation of a raffle to fetch the pending images that the user creating the raffle uploaded.
     * 
     * @return the user's images
     */
    UserImagesResponse getAllUserImages();

    /**
     * Fetches all images for a user for a specific raffle.
     * Useful during the edition of a raffle to fetch the pending images that the user editing the raffle uploaded.
     * 
     * @param associationId the ID of the association
     * @param raffleId the ID of the raffle
     * @return the user's images for the raffle
     */
    UserImagesResponse getAllUserImagesForRaffle(Long associationId, Long raffleId);
}