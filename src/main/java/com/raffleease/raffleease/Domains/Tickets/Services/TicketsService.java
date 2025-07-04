package com.raffleease.raffleease.Domains.Tickets.Services;

import com.raffleease.raffleease.Domains.Carts.Model.Cart;
import com.raffleease.raffleease.Domains.Customers.Model.Customer;
import com.raffleease.raffleease.Domains.Raffles.Model.Raffle;
import com.raffleease.raffleease.Domains.Tickets.DTO.TicketsCreate;
import com.raffleease.raffleease.Domains.Tickets.Model.Ticket;
import com.raffleease.raffleease.Domains.Tickets.Model.TicketStatus;

import java.util.List;

public interface TicketsService {
    /**
     * Creates a new ticket for a raffle.
     * Used during the order creation process.
     * 
     * @param raffle the raffle to create the ticket for
     * @param request the request to create the ticket
     * @return the created ticket
     */
    List<Ticket> create(Raffle raffle, TicketsCreate request);

    /**
     * Releases tickets back to the available pool from a cart.
     * 
     * @param tickets the tickets to release
     */
    void releaseTickets(List<Ticket> tickets);

    /**
     * Reserves tickets for a cart.
     * Used during the order creation process.
     * 
     * @param cart the cart to reserve the tickets for
     * @param tickets the tickets to reserve
     */
    void reserveTickets(Cart cart, List<Ticket> tickets);

    /**
     * Transfers tickets to a customer when an order is completed.
     * 
     * @param cartTickets the tickets to transfer
     * @param customer the customer to transfer the tickets to
     * @return the transferred tickets
     */
    List<Ticket> transferTicketsToCustomer(List<Ticket> cartTickets, Customer customer);

    /**
     * Updates the status of a list of tickets.
     * 
     * @param tickets the tickets to update
     * @param status the status to update the tickets to
     * @return the updated tickets
     */
    List<Ticket> updateStatus(List<Ticket> tickets, TicketStatus status);
}