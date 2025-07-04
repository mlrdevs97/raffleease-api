package com.raffleease.raffleease.Domains.Images.Services.Impls;

import com.raffleease.raffleease.Domains.Images.Model.Image;
import com.raffleease.raffleease.Domains.Images.Model.ImageStatus;
import com.raffleease.raffleease.Domains.Images.Repository.ImagesRepository;
import com.raffleease.raffleease.Domains.Images.Services.ImagesDeleteService;
import com.raffleease.raffleease.Common.Exceptions.CustomExceptions.DatabaseException;
import com.raffleease.raffleease.Domains.Images.Services.ImagesService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import java.util.List;

import static com.raffleease.raffleease.Domains.Images.Model.ImageStatus.MARKED_FOR_DELETION;

@RequiredArgsConstructor
@Service
public class ImagesDeleteServiceImpl implements ImagesDeleteService {
    private final ImagesService imagesService;
    private final ImagesRepository repository;

    @Override
    public void deleteAll(List<Image> images) {
        try {
            repository.deleteAll(images);
        } catch (DataAccessException ex) {
            throw new DatabaseException("Database error occurred while deleting images: " + ex.getMessage());
        }
    }

    @Override
    public void delete(Image image) {
        try {
            repository.delete(image);
        } catch (DataAccessException ex) {
            throw new DatabaseException("Database error occurred while deleting image: " + ex.getMessage());
        }
    }

    @Override
    public void softDelete(Long id) {
        Image image = imagesService.findById(id);
        image.setStatus(MARKED_FOR_DELETION);
        repository.save(image);
    }
}
