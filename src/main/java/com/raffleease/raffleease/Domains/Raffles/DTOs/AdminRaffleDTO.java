package com.raffleease.raffleease.Domains.Raffles.DTOs;

import java.math.BigDecimal;

import com.raffleease.raffleease.Domains.Associations.DTO.AssociationDTO;
import com.raffleease.raffleease.Domains.Raffles.Model.RaffleStatus;
import java.time.LocalDateTime;
import java.util.List;

public record AdminRaffleDTO(
        Long id,
        String title,
        String description,
        String url,
        LocalDateTime startDate,
        LocalDateTime endDate,
        RaffleStatus status,
        List<String> imageKeys,
        BigDecimal ticketPrice,
        Long firstTicketNumber,
        Long availableTickets,
        Long totalTickets,
        AssociationDTO association,
        BigDecimal revenue,
        Long soldTickets
) { }
