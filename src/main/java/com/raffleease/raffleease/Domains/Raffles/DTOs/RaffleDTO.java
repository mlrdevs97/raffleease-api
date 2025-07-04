package com.raffleease.raffleease.Domains.Raffles.DTOs;

import com.raffleease.raffleease.Domains.Images.DTOs.ImageDTO;
import com.raffleease.raffleease.Domains.Raffles.Model.CompletionReason;
import com.raffleease.raffleease.Domains.Raffles.Model.RaffleStatus;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Builder
public record RaffleDTO(
        Long id,
        Long associationId,
        String title,
        String description,
        RaffleStatus status,
        BigDecimal ticketPrice,
        Long totalTickets,
        Long firstTicketNumber,
        List<ImageDTO> images,
        CompletionReason completionReason,
        Long winningTicketId,
        RaffleStatisticsDTO statistics,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime startDate,
        LocalDateTime endDate,
        LocalDateTime completedAt
) { }