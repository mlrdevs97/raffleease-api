package com.raffleease.raffleease.Domains.Tickets.Repository;

import com.raffleease.raffleease.Domains.Tickets.DTO.TicketsSearchFilters;
import com.raffleease.raffleease.Domains.Tickets.Model.Ticket;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface TicketsSearchRepository {
    Page<Ticket> search(TicketsSearchFilters searchFilters, Long associationId, Long raffleId, Pageable pageable);
}
