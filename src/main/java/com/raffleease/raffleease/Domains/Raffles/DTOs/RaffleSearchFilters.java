package com.raffleease.raffleease.Domains.Raffles.DTOs;

import com.raffleease.raffleease.Domains.Raffles.Model.RaffleStatus;

public record RaffleSearchFilters(
    String title,
    RaffleStatus status
) {
    public RaffleSearchFilters {
        title = title != null ? title.trim() : null;
    }
} 