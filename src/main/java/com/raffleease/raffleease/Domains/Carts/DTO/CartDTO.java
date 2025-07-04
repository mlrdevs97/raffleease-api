package com.raffleease.raffleease.Domains.Carts.DTO;

import com.raffleease.raffleease.Domains.Carts.Model.CartStatus;
import com.raffleease.raffleease.Domains.Tickets.DTO.TicketDTO;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

@Builder
public record CartDTO(
        Long id,
        Long userId,
        List<TicketDTO> tickets,
        CartStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) { }
