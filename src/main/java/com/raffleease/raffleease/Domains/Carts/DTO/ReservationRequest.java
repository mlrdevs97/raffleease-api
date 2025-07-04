package com.raffleease.raffleease.Domains.Carts.DTO;

import jakarta.validation.constraints.NotEmpty;
import lombok.Builder;

import java.util.List;

@Builder
public record ReservationRequest (
        @NotEmpty
        List<Long> ticketIds
) { }