package com.raffleease.raffleease.Domains.Images.Mappers.Impls;

import com.raffleease.raffleease.Domains.Images.DTOs.ImageDTO;
import com.raffleease.raffleease.Domains.Images.Mappers.ImagesMapper;
import com.raffleease.raffleease.Domains.Images.Model.Image;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@RequiredArgsConstructor
@Service
public class ImagesMapperImpl implements ImagesMapper {
    @Override
    public List<ImageDTO> fromImagesList(List<Image> images) {
        return images.stream()
                .map(image -> ImageDTO.builder()
                        .id(image.getId())
                        .fileName(image.getFileName())
                        .filePath(image.getFilePath())
                        .contentType(image.getContentType())
                        .url(image.getUrl())
                        .imageOrder(image.getImageOrder())
                        .build()
                ).toList();
    }
}
