package com.raffleease.raffleease.Domains.Tickets.DTO;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record TicketsCreate (
    @NotNull
    @Positive
    Long amount,

    @NotNull
    @Positive
    BigDecimal price,

    @NotNull
    @Min(value = 0)
    Long lowerLimit
){}
