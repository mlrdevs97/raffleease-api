package com.raffleease.raffleease.Domains.Images.DTOs;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

import static com.raffleease.raffleease.Common.Constants.Constants.MAX_IMAGES;
import static com.raffleease.raffleease.Common.Constants.Constants.MIN_IMAGES;

public record ImageUpload(
        @NotEmpty
        @Size(min = MIN_IMAGES, max = MAX_IMAGES)
        List<MultipartFile> files
) {}