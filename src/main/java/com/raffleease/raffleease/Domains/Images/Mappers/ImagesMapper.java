package com.raffleease.raffleease.Domains.Images.Mappers;

import com.raffleease.raffleease.Domains.Images.DTOs.ImageDTO;
import com.raffleease.raffleease.Domains.Images.Model.Image;

import java.util.List;

public interface ImagesMapper {
    List<ImageDTO> fromImagesList(List<Image> images);
}
