package com.raffleease.raffleease.Domains.Raffles.DTOs;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Builder
public record RaffleStatisticsDTO (
        Long id,
        Long raffleId,
        Long availableTickets,
        Long soldTickets,
        BigDecimal revenue,
        BigDecimal averageOrderValue,
        Long totalOrders,
        Long completedOrders,
        Long pendingOrders,
        Long cancelledOrders,
        Long unpaidOrders,
        Long refundedOrders,
        Long participants,
        BigDecimal ticketsPerParticipant,
        LocalDateTime firstSaleDate,
        LocalDateTime lastSaleDate,
        BigDecimal dailySalesVelocity
) {}