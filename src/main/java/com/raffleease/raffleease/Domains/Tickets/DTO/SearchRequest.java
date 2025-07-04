package com.raffleease.raffleease.Domains.Tickets.DTO;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;

@Builder
public record SearchRequest(
        @NotNull
        String ticketNumber,

        @NotNull
        Long raffleId
) {}