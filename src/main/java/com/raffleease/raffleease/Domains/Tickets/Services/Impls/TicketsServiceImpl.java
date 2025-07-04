package com.raffleease.raffleease.Domains.Tickets.Services.Impls;

import com.raffleease.raffleease.Domains.Carts.Model.Cart;
import com.raffleease.raffleease.Domains.Customers.Model.Customer;
import com.raffleease.raffleease.Domains.Raffles.Model.Raffle;
import com.raffleease.raffleease.Domains.Tickets.DTO.TicketsCreate;
import com.raffleease.raffleease.Domains.Tickets.Model.Ticket;
import com.raffleease.raffleease.Domains.Tickets.Model.TicketStatus;
import com.raffleease.raffleease.Domains.Tickets.Repository.TicketsRepository;
import com.raffleease.raffleease.Domains.Tickets.Services.TicketsService;
import com.raffleease.raffleease.Common.Exceptions.CustomExceptions.DatabaseException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

import static com.raffleease.raffleease.Domains.Tickets.Model.TicketStatus.*;

@RequiredArgsConstructor
@Service
public class TicketsServiceImpl implements TicketsService {
    private final TicketsRepository repository;

    @Override
    public List<Ticket> create(Raffle raffle, TicketsCreate request) {
        long upperLimit = request.lowerLimit() + request.amount() - 1;
        return LongStream.rangeClosed(request.lowerLimit(), upperLimit)
                .mapToObj(i -> Ticket.builder()
                        .status(AVAILABLE)
                        .ticketNumber(Long.toString(i))
                        .raffle(raffle)
                        .build()
                ).collect(Collectors.toCollection(ArrayList::new));
    }

    @Override
    public void releaseTickets(List<Ticket> tickets) {
        if (tickets == null || tickets.isEmpty()) {
            return;
        }
        saveAll(tickets.stream().peek(ticket -> {
            ticket.setStatus(AVAILABLE);
            ticket.setCustomer(null);
            ticket.setCart(null);
        }).toList());
    }

    @Override
    public void reserveTickets(Cart cart, List<Ticket> tickets) {
        tickets.forEach(ticket -> {
            ticket.setStatus(RESERVED);
            ticket.setCart(cart);
        });
    }

    /**
     * Transfers tickets from cart to customer and removes cart from tickets.
     * This is different from releasing - tickets go to customer instead of back to available pool.
     */
    @Override
    public List<Ticket> transferTicketsToCustomer(List<Ticket> tickets, Customer customer) {
        return saveAll(tickets.stream().peek(ticket -> {
            ticket.setCustomer(customer);
            ticket.setCart(null);
        }).toList());
    }

    @Override
    public List<Ticket> updateStatus(List<Ticket> tickets, TicketStatus status) {
        return tickets.stream().peek(ticket -> ticket.setStatus(status)).toList();
    }

    private List<Ticket> saveAll(List<Ticket> entities) {
        try {
            return repository.saveAll(entities);
        } catch (DataAccessException ex) {
            throw new DatabaseException("Database error occurred while saving entities: " + ex.getMessage());
        }
    }
}
