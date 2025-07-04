package com.raffleease.raffleease.Domains.Images.DTOs;

import lombok.Builder;

import java.util.List;

@Builder
public record UserImagesResponse(
        List<ImageDTO> images
) { }
