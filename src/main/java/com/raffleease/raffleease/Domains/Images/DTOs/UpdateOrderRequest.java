package com.raffleease.raffleease.Domains.Images.DTOs;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Builder;

import java.util.List;

import static com.raffleease.raffleease.Common.Constants.Constants.MAX_IMAGES;
import static com.raffleease.raffleease.Common.Constants.Constants.MIN_IMAGES;

@Builder
public record UpdateOrderRequest(
        @NotEmpty
        @Size(min = MIN_IMAGES, max = MAX_IMAGES)
        @Valid
        List<ImageDTO> images
) { }
