package com.raffleease.raffleease.Domains.Tickets.Mappers.Impls;

import com.raffleease.raffleease.Domains.Tickets.DTO.TicketDTO;
import com.raffleease.raffleease.Domains.Tickets.Mappers.TicketsMapper;
import com.raffleease.raffleease.Domains.Tickets.Model.Ticket;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class TicketsMapperImpl implements TicketsMapper {
    public TicketDTO fromTicket(Ticket ticket) {
        return TicketDTO.builder()
                .id(ticket.getId())
                .ticketNumber(ticket.getTicketNumber())
                .status(ticket.getStatus())
                .raffleId(ticket.getRaffle() != null ? ticket.getRaffle().getId() : null)
                .customerId(ticket.getCustomer() != null ? ticket.getCustomer().getId() : null)
                .createdAt(ticket.getCreatedAt())
                .updatedAt(ticket.getUpdatedAt())
                .build();
    }

    public List<TicketDTO> fromTicketList(List<Ticket> tickets) {
        return tickets.stream()
                .map(this::fromTicket)
                .collect(Collectors.toList());
    }
}