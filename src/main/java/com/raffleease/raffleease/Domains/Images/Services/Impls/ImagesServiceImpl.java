package com.raffleease.raffleease.Domains.Images.Services.Impls;

import com.raffleease.raffleease.Common.Exceptions.CustomExceptions.DatabaseException;
import com.raffleease.raffleease.Common.Exceptions.CustomExceptions.NotFoundException;
import com.raffleease.raffleease.Domains.Images.DTOs.ImageDTO;
import com.raffleease.raffleease.Domains.Images.DTOs.UserImagesResponse;
import com.raffleease.raffleease.Domains.Images.Mappers.ImagesMapper;
import com.raffleease.raffleease.Domains.Images.Model.Image;
import com.raffleease.raffleease.Domains.Images.Repository.ImagesRepository;
import com.raffleease.raffleease.Domains.Images.Services.FileStorageService;
import com.raffleease.raffleease.Domains.Images.Services.ImagesService;
import com.raffleease.raffleease.Domains.Images.Validators.ImagesValidator;
import com.raffleease.raffleease.Domains.Raffles.Model.Raffle;
import com.raffleease.raffleease.Domains.Raffles.Services.RafflesPersistenceService;
import com.raffleease.raffleease.Domains.Users.Model.User;
import com.raffleease.raffleease.Domains.Users.Services.UsersService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import java.util.List;

import static com.raffleease.raffleease.Domains.Images.Model.ImageStatus.MARKED_FOR_DELETION;
import static com.raffleease.raffleease.Domains.Images.Model.ImageStatus.PENDING;

@RequiredArgsConstructor
@Service
public class ImagesServiceImpl implements ImagesService {
    private final FileStorageService fileStorageService;
    private final ImagesRepository repository;
    private final UsersService usersService;
    private final RafflesPersistenceService rafflesPersistence;
    private final ImagesMapper mapper;
    private final ImagesValidator validator;

    @Value("${spring.application.hosts.server}")
    private String host;

    @Override
    public List<Image> saveAll(List<Image> images) {
        try {
            return repository.saveAll(images);
        } catch (DataAccessException ex) {
            throw new DatabaseException("Database error occurred while saving images: " + ex.getMessage());
        }
    }

    @Override
    public Image findById(Long id) {
        try {
            return repository.findById(id).orElseThrow(() -> new NotFoundException("Image not found for id <" + id + ">"));
        } catch (DataAccessException ex) {
            throw new DatabaseException("Database error occurred while retrieving image with ID <" + id + ">: " + ex.getMessage());
        }
    }

    @Override
    public List<Image> findAllById(List<Long> ids) {
        try {
            List<Image> images = repository.findAllById(ids);
            if(images.isEmpty()) throw new NotFoundException("No images were found for provided ids");
            return images;
        } catch (DataAccessException ex) {
            throw new DatabaseException("Database error occurred while retrieving images by ID: " + ex.getMessage());
        }
    }

    @Override
    public Resource getFile(Long id) {
        Image image = findById(id);
        if (image.getStatus() == MARKED_FOR_DELETION) {
            throw new NotFoundException("Image not found for id <" + id + ">");
        }
        return fileStorageService.load(image.getFilePath());
    }

    @Override
    public UserImagesResponse getAllUserImages() {
        User user = usersService.getAuthenticatedUser();
        List<Image> userImages = repository.findAllByUserAndStatus(user, PENDING);
        List<ImageDTO> mappedImages = mapper.fromImagesList(userImages);
        return UserImagesResponse.builder()
                .images(mappedImages)
                .build();
    }

    @Override
    public UserImagesResponse getAllUserImagesForRaffle(Long associationId, Long raffleId) {
        User user = usersService.getAuthenticatedUser();
        Raffle raffle = rafflesPersistence.findById(raffleId);
        validator.validateRaffleBelongsToAssociation(associationId, raffle);
        List<Image> userImages = repository.findAllByUserAndRaffleAndStatus(user, raffle, PENDING);
        List<ImageDTO> mappedImages = mapper.fromImagesList(userImages);
        return UserImagesResponse.builder()
                .images(mappedImages)
                .build();
    }
}