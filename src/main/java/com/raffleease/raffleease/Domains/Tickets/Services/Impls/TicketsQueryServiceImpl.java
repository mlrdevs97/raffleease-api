package com.raffleease.raffleease.Domains.Tickets.Services.Impls;

import com.raffleease.raffleease.Common.Exceptions.ErrorCodes;
import com.raffleease.raffleease.Domains.Carts.Model.Cart;
import com.raffleease.raffleease.Domains.Raffles.Model.Raffle;
import com.raffleease.raffleease.Domains.Raffles.Services.RafflesPersistenceService;
import com.raffleease.raffleease.Domains.Tickets.DTO.TicketDTO;
import com.raffleease.raffleease.Domains.Tickets.DTO.TicketsSearchFilters;
import com.raffleease.raffleease.Domains.Tickets.Mappers.TicketsMapper;
import com.raffleease.raffleease.Domains.Tickets.Model.Ticket;
import com.raffleease.raffleease.Domains.Tickets.Model.TicketStatus;
import com.raffleease.raffleease.Domains.Tickets.Repository.TicketsSearchRepository;
import com.raffleease.raffleease.Domains.Tickets.Repository.TicketsRepository;
import com.raffleease.raffleease.Domains.Tickets.Services.TicketsQueryService;
import com.raffleease.raffleease.Common.Exceptions.CustomExceptions.BusinessException;
import com.raffleease.raffleease.Common.Exceptions.CustomExceptions.DatabaseException;
import com.raffleease.raffleease.Common.Exceptions.CustomExceptions.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

import static com.raffleease.raffleease.Common.Exceptions.ErrorCodes.INSUFFICIENT_TICKETS_AVAILABLE;
import static com.raffleease.raffleease.Domains.Tickets.Model.TicketStatus.AVAILABLE;

@RequiredArgsConstructor
@Service
public class TicketsQueryServiceImpl implements TicketsQueryService {
    private final RafflesPersistenceService rafflePersistence;
    private final TicketsRepository repository;
    private final TicketsSearchRepository customRepository;
    private final TicketsMapper mapper;

    @Override
    public List<Ticket> findAllById(List<Long> ticketIds) {
        List<Ticket> tickets = repository.findAllById(ticketIds);
        if (tickets.isEmpty() || tickets.size() < ticketIds.size()) {
            throw new NotFoundException("Some tickets were not found");
        }
        return tickets;
    }

    @Override
    public Page<TicketDTO> search(Long associationId, Long raffleId, TicketsSearchFilters searchFilters, Pageable pageable) {
        Page<Ticket> ticketsPage = customRepository.search(searchFilters, associationId, raffleId, pageable);
        return ticketsPage.map(mapper::fromTicket);
    }

    @Override
    public List<Ticket> findByRaffleAndStatus(Raffle raffle, TicketStatus ticketStatus) {
        try {
            return repository.findByRaffleAndStatus(raffle, AVAILABLE);
        } catch (DataAccessException ex) {
            throw new DatabaseException("Database error occurred while retrieving tickets: " + ex.getMessage());
        }
    }

    @Override
    public List<TicketDTO> getRandom(Long raffleId, Long quantity) {
        Raffle raffle = rafflePersistence.findById(raffleId);
        List<Ticket> availableTickets = findByRaffleAndStatus(raffle, AVAILABLE);
        validateTicketAvailability(availableTickets, quantity);
        List<Ticket> selectedTickets = selectRandomTickets(availableTickets, quantity);
        return mapper.fromTicketList(selectedTickets);
    }

    @Override
    public List<Ticket> findAllByCart(Cart cart) {
        try {
            return repository.findAllByCart(cart);
        } catch (DataAccessException ex) {
            throw new DatabaseException("Database error occurred while retrieving tickets: " + ex.getMessage());
        }
    }

    private void validateTicketAvailability(List<Ticket> availableTickets, Long requestedQuantity) {
        if (availableTickets.isEmpty() || availableTickets.size() < requestedQuantity) {
            throw new BusinessException("Not enough tickets were found for this order", INSUFFICIENT_TICKETS_AVAILABLE);
        }
    }

    private List<Ticket> selectRandomTickets(List<Ticket> availableTickets, Long quantity) {
        Collections.shuffle(availableTickets);
        return availableTickets.subList(0, quantity.intValue());
    }
}
