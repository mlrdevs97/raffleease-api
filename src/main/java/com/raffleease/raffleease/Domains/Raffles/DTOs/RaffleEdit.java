package com.raffleease.raffleease.Domains.Raffles.DTOs;

import com.raffleease.raffleease.Domains.Images.DTOs.ImageDTO;
import jakarta.validation.constraints.*;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static com.raffleease.raffleease.Common.Constants.Constants.MAX_IMAGES;
import static com.raffleease.raffleease.Common.Constants.Constants.MIN_IMAGES;

@Builder
public record RaffleEdit(
        @Size(max = 100)
        String title,

        @Size(max = 5000)
        String description,

        @Future
        LocalDateTime endDate,

        @Size(min = MIN_IMAGES, max = MAX_IMAGES)
        List<ImageDTO> images,

        @DecimalMin(value = "0.0", inclusive = false)
        BigDecimal ticketPrice,

        @Positive
        Long totalTickets
) { }