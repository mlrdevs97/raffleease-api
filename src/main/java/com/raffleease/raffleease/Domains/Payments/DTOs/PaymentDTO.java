package com.raffleease.raffleease.Domains.Payments.DTOs;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Builder
public record PaymentDTO(
        String paymentMethod,
        BigDecimal total,
        String paymentIntentId,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime completedAt
) {
}
