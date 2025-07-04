package com.raffleease.raffleease.Domains.Images.DTOs;

import jakarta.validation.constraints.*;
import lombok.Builder;

@Builder
public record ImageDTO(
        @NotNull
        Long id,

        @NotBlank
        String fileName,

        @NotBlank
        String filePath,

        @NotBlank
        String contentType,

        @NotBlank
        String url,

        @NotNull
        @Min(value = 0)
        @Max(value = 10)
        Integer imageOrder
) { }