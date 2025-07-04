package com.raffleease.raffleease.Domains.Images.Services.Impls;

import com.raffleease.raffleease.Domains.Associations.Model.Association;
import com.raffleease.raffleease.Domains.Associations.Services.AssociationsService;
import com.raffleease.raffleease.Domains.Images.DTOs.ImageDTO;
import com.raffleease.raffleease.Domains.Images.DTOs.ImageResponse;
import com.raffleease.raffleease.Domains.Images.DTOs.ImageUpload;
import com.raffleease.raffleease.Domains.Images.Mappers.ImagesMapper;
import com.raffleease.raffleease.Domains.Images.Model.Image;
import com.raffleease.raffleease.Domains.Images.Repository.ImagesRepository;
import com.raffleease.raffleease.Domains.Images.Services.FileStorageService;
import com.raffleease.raffleease.Domains.Images.Services.ImagesCreateService;
import com.raffleease.raffleease.Domains.Images.Services.ImagesService;
import com.raffleease.raffleease.Domains.Images.Validators.ImagesValidator;
import com.raffleease.raffleease.Domains.Raffles.Model.Raffle;
import com.raffleease.raffleease.Domains.Raffles.Services.RafflesPersistenceService;
import com.raffleease.raffleease.Common.Exceptions.CustomExceptions.FileStorageException;
import com.raffleease.raffleease.Domains.Users.Model.User;
import com.raffleease.raffleease.Domains.Users.Services.UsersService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

import static com.raffleease.raffleease.Domains.Images.Model.ImageStatus.PENDING;

@RequiredArgsConstructor
@Service
@Slf4j
public class ImagesCreateServiceImpl implements ImagesCreateService {
    private final ImagesService imagesService;
    private final FileStorageService fileStorageService;
    private final ImagesRepository repository;
    private final ImagesMapper mapper;
    private final AssociationsService associationsService;
    private final RafflesPersistenceService rafflesPersistenceService;
    private final ImagesValidator imagesValidator;
    private final UsersService usersService;

    @Value("${spring.application.hosts.server}")
    private String host;

    /**
     * Create images still not associated to a raffle.
     * Used to upload images when creating a new raffle
     *  
     * @param associationId Association ID
     * @param uploadRequest Upload request
     * @return Image response
     */
    @Override
    @Transactional
    public ImageResponse create(Long associationId, ImageUpload uploadRequest) {
        String baseURL = host + "/v1/public/associations/" + associationId + "/images/";
        return processImagesCreation(associationId, uploadRequest, 0, baseURL, null);
    }

    /**
     * Create images for an existing raffle
     * Used to upload images when editing an existing raffle
     * 
     * @param associationId Association ID
     * @param raffleId Raffle ID
     * @param uploadRequest Upload request
     * @return Image response
     */
    @Override
    @Transactional
    public ImageResponse create(Long associationId, Long raffleId, ImageUpload uploadRequest) {
        Raffle raffle = rafflesPersistenceService.findById(raffleId);
        imagesValidator.validateRaffleBelongsToAssociation(associationId, raffle);
        int currentImagesCount = raffle.getImages().size();
        String baseURL = host + "/v1/public/associations/" + associationId + "/raffles/" + raffleId + "/images/";
        return processImagesCreation(associationId, uploadRequest, currentImagesCount, baseURL, raffle);
    }

    /**
     * Process images creation for a new or existing raffle.
     * Used to upload images when creating a new or editing an existing raffle.
     * 
     * @param associationId Association ID
     * @param uploadRequest Upload request
     * @param currentImagesCount Current images count
     * @param baseURL Base URL
     * @param raffle Raffle
     * @return Image response
     */
    private ImageResponse processImagesCreation(Long associationId, ImageUpload uploadRequest, int currentImagesCount, String baseURL, Raffle raffle) {
        Association association = associationsService.findById(associationId);
        User user = usersService.getAuthenticatedUser();
        List<MultipartFile> files = uploadRequest.files();
        int pendingImagesCount = repository.countImagesByUserAndStatus(user, PENDING);
        int totalImagesCount = pendingImagesCount + currentImagesCount;
        imagesValidator.validateTotalImagesNumber(files.size(), totalImagesCount);
        String batchId = UUID.randomUUID().toString();
        List<String> tempFilePaths = null;
        List<String> finalFilePaths = null;
        List<Image> savedImages = null;
        
        try {
            // 1: Store files to temporary location first
            tempFilePaths = fileStorageService.saveTemporaryBatch(files, String.valueOf(associationId), batchId);
            // 2: Create Image entities in database
            savedImages = createImageEntities(association, user, files, raffle);
            // 3: Set image order based on both pending and existing images in the raffle
            setImagesOrder(savedImages, currentImagesCount, pendingImagesCount);
            // 4: Move files to final location using database IDs
            List<String> imageIds = savedImages.stream().map(img -> String.valueOf(img.getId())).toList();
            String raffleId = raffle != null ? String.valueOf(raffle.getId()) : null;
            finalFilePaths = fileStorageService.moveTemporaryBatchToFinal(tempFilePaths, String.valueOf(associationId), raffleId, imageIds);
            // 5: Update Image entities with final file paths and URLs
            updateImageEntitiesWithFilePaths(savedImages, finalFilePaths, baseURL);
            List<Image> updatedImages = imagesService.saveAll(savedImages);
            // 6: Convert to DTOs and return
            List<ImageDTO> mappedImages = mapper.fromImagesList(updatedImages);
            return new ImageResponse(mappedImages);
            
        } catch (Exception ex) {
            // Clean up any saved files
            rollbackImageCreation(tempFilePaths, finalFilePaths, batchId);

            if (savedImages != null && !savedImages.isEmpty()) {
                repository.deleteAll(savedImages);
            }
            
            if (ex instanceof FileStorageException) {
                throw ex;
            } else {
                throw new FileStorageException("Image creation failed: " + ex.getMessage());
            }
        }
    }

    /**
     * Create Image entities in database
     * The images are created without file paths or URLs
     * They are later updated with the file paths and URLs
     * 
     * @param user User
     * @param files List of files to create images from
     * @param raffle Raffle
     * @return List of created images
     */
    private List<Image> createImageEntities(Association association, User user, List<MultipartFile> files, Raffle raffle) {
        return imagesService.saveAll(files.stream()
                .map(file -> Image.builder()
                        .fileName(file.getOriginalFilename())
                        .contentType(file.getContentType())
                        .status(PENDING)
                        .association(association)
                        .user(user)
                        .raffle(raffle)
                        .build())
                .toList());
    }

    /**
     * Set image order based on both pending and existing images in the raffle
     * 
     * @param images List of images to set the order for
     * @param currentImagesCount Number of existing images in the raffle
     * @param pendingImagesCount Number of pending images in the database
     */
    private void setImagesOrder(List<Image> images, int currentImagesCount, int pendingImagesCount) {
        int startingOrder = currentImagesCount + pendingImagesCount;
        for (int i = 0; i < images.size(); i++) {
            images.get(i).setImageOrder(startingOrder + i + 1);
        }
    }

    /**
     * Update Image entities with final file paths and URLs
     * 
     * @param images List of images to update
     * @param filePaths List of file paths to set
     * @param baseURL Base URL for the images
     */
    private void updateImageEntitiesWithFilePaths(List<Image> images, List<String> filePaths, String baseURL) {
        if (images.size() != filePaths.size()) {
            throw new IllegalArgumentException("Number of images must match number of file paths");
        }
        
        for (int i = 0; i < images.size(); i++) {
            Image image = images.get(i);
            String filePath = filePaths.get(i);
            image.setFilePath(filePath);
            image.setUrl(baseURL + image.getId());
        }
    }

    /**
     * Rollback image creation by cleaning up any saved files
     * 
     * @param tempFilePaths List of temporary file paths to clean up
     * @param finalFilePaths List of final file paths to clean up
     * @param batchId Batch ID for logging
     */
    private void rollbackImageCreation(List<String> tempFilePaths, List<String> finalFilePaths, String batchId) {
        try {
            if (finalFilePaths != null && !finalFilePaths.isEmpty()) {
                fileStorageService.cleanupFiles(finalFilePaths);
            }
            if (tempFilePaths != null && !tempFilePaths.isEmpty()) {
                fileStorageService.cleanupTemporaryFiles(tempFilePaths);
            }
        } catch (Exception cleanupException) {
            log.error("Error during rollback cleanup for batch: {}", batchId, cleanupException);
        }
    }
}