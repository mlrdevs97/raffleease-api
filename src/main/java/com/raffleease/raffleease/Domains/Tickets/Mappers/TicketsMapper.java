package com.raffleease.raffleease.Domains.Tickets.Mappers;

import com.raffleease.raffleease.Domains.Tickets.DTO.TicketDTO;
import com.raffleease.raffleease.Domains.Tickets.Model.Ticket;

import java.util.List;

public interface TicketsMapper {
    List<TicketDTO> fromTicketList(List<Ticket> tickets);
    TicketDTO fromTicket(Ticket ticket);
}