package com.raffleease.raffleease.Domains.Tickets.DTO;

import com.raffleease.raffleease.Domains.Tickets.Model.TicketStatus;

public record TicketsSearchFilters(
        String ticketNumber,
        TicketStatus status,
        Long customerId
) {
    public TicketsSearchFilters {
        ticketNumber = ticketNumber != null ? ticketNumber.trim() : null;
    }
}
