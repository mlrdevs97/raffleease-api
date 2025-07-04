package com.raffleease.raffleease.Domains.Images.Services;

import com.raffleease.raffleease.Domains.Images.DTOs.ImageDTO;
import com.raffleease.raffleease.Domains.Images.Model.Image;
import com.raffleease.raffleease.Domains.Raffles.Model.Raffle;

import java.util.List;

public interface ImagesAssociateService {

    /**
     * Associates images to a raffle on creation once the raffle creation process is completed.
     *
     * @param raffle    the raffle to associate the images to
     * @param imageDTOs the images to associate to the raffle
     * @return the associated images
     */
    List<Image> associateImagesToRaffleOnCreate(Raffle raffle, List<ImageDTO> imageDTOs);

    /**
     * Associates images to a raffle on edition once the raffle edition process is completed.
     *
     * @param raffle    the raffle to associate the images to
     * @param imageDTOs the images to associate to the raffle
     * @return the associated images
     */
    List<Image> associateImagesToRaffleOnEdit(Raffle raffle, List<ImageDTO> imageDTOs);

    /**
     * Set the final image paths and URLs of the images just after the raffle creation process has completed.
     *
     * @param raffle the raffle to set the final image paths and URLs of the images associated with
     * @param images the images to set the final image paths and URLs of
     */
    void finalizeImagePathsAndUrls(Raffle raffle, List<Image> images);
}
