package com.raffleease.raffleease.Domains.Images.Services;

import com.raffleease.raffleease.Domains.Images.DTOs.ImageResponse;
import com.raffleease.raffleease.Domains.Images.DTOs.ImageUpload;

public interface ImagesCreateService {
    /**
     * Creates a new image in the context of the creation of a new raffle.
     * The images are associated with the user uploading them until the raffle creation is completed.
     * An user can upload a maximum of 10 images at a time for the same raffle being created.
     * Only ADMIN and MEMBER users can create images.
     * 
     * @param associationId the ID of the association
     * @param uploadRequest the binary data of the image to be created
     * @return the created image
     */
    ImageResponse create(Long associationId, ImageUpload uploadRequest);

    /**
     * Creates a new image in the context of the edition of an existing raffle.
     * The images are directly associated with the raffle and also with the user uploading them until the raffle edition is completed.
     * An user can upload a maximum of 10 images at a time for the same raffle being edited, including the ones already associated with the raffle.
     * Only ADMIN and MEMBER users can create images.
     * 
     * @param associationId the ID of the association
     * @param raffleId the ID of the raffle
     * @param uploadRequest the binary data of the image to be created
     * @return the created image
     */
    ImageResponse create(Long associationId, Long raffleId, ImageUpload uploadRequest);
}
