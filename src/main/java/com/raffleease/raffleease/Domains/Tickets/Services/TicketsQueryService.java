package com.raffleease.raffleease.Domains.Tickets.Services;

import com.raffleease.raffleease.Domains.Carts.Model.Cart;
import com.raffleease.raffleease.Domains.Raffles.Model.Raffle;
import com.raffleease.raffleease.Domains.Tickets.DTO.TicketDTO;
import com.raffleease.raffleease.Domains.Tickets.DTO.TicketsSearchFilters;
import com.raffleease.raffleease.Domains.Tickets.Model.Ticket;
import com.raffleease.raffleease.Domains.Tickets.Model.TicketStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface TicketsQueryService {
    /**
     * Fetches all tickets by their IDs.
     * 
     * @param ticketsIds the IDs of the tickets to fetch
     * @return a list of tickets
     */
    List<Ticket> findAllById(List<Long> ticketsIds);

    /**
     * Searches for tickets based on the provided filters.
     * 
     * @param associationId the ID of the association to search for tickets
     * @param raffleId the ID of the raffle to search for tickets
     * @param searchFilters the filters to apply to the search
     * @param pageable the pagination information
     * @return a page of tickets matching the search criteria
     */
    Page<TicketDTO> search(Long associationId, Long raffleId, TicketsSearchFilters searchFilters, Pageable pageable);

    /**
     * Fetches all tickets by raffle and status.
     * 
     * @param raffle the raffle to search for tickets
     * @param status the status of the tickets to fetch
     * @return a list of tickets
     */
    List<Ticket> findByRaffleAndStatus(Raffle raffle, TicketStatus status);

    /**
     * Fetches a random number of tickets by raffle.
     * 
     * @param raffleId the ID of the raffle to search for tickets
     * @param quantity the number of tickets to fetch
     * @return a list of tickets
     */
    List<TicketDTO> getRandom(Long raffleId, Long quantity);

    /**
     * Fetches all tickets of the provided cart.
     * 
     * @param cart the cart to search for tickets
     * @return a list of tickets
    */
    List<Ticket> findAllByCart(Cart cart);
}
