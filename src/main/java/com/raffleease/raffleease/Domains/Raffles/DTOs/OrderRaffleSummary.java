package com.raffleease.raffleease.Domains.Raffles.DTOs;

import com.raffleease.raffleease.Domains.Raffles.Model.RaffleStatus;
import lombok.Builder;

@Builder
public record OrderRaffleSummary(
        Long id,
        String title,
        String imageURL,
        RaffleStatus status
) {}
