package com.raffleease.raffleease.Domains.Orders.DTOs;

import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record OrderItemDTO(
        Long id,
        String ticketNumber,
        BigDecimal priceAtPurchase,
        Long ticketId,
        Long raffleId,
        Long customerId
) {}
