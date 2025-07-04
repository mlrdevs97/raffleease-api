package com.raffleease.raffleease.Domains.Images.DTOs;

import lombok.Builder;

import java.util.List;

@Builder
public record ImageResponse(
        List<ImageDTO> images
) { }
