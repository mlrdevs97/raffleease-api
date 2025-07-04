package com.raffleease.raffleease.Domains.Raffles.Services.Impl;

import com.raffleease.raffleease.Domains.Raffles.DTOs.RaffleDTO;
import com.raffleease.raffleease.Domains.Raffles.DTOs.StatusUpdate;
import com.raffleease.raffleease.Domains.Raffles.Mappers.RafflesMapper;
import com.raffleease.raffleease.Domains.Raffles.Model.CompletionReason;
import com.raffleease.raffleease.Domains.Raffles.Model.Raffle;
import com.raffleease.raffleease.Domains.Raffles.Model.RaffleStatus;
import com.raffleease.raffleease.Domains.Raffles.Services.RafflesPersistenceService;
import com.raffleease.raffleease.Domains.Raffles.Services.RafflesStatusService;
import com.raffleease.raffleease.Common.Exceptions.CustomExceptions.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Objects;

import static com.raffleease.raffleease.Domains.Raffles.Model.CompletionReason.*;
import static com.raffleease.raffleease.Domains.Raffles.Model.RaffleStatus.*;
import static com.raffleease.raffleease.Domains.Tickets.Model.TicketStatus.SOLD;

@RequiredArgsConstructor
@Service
public class RafflesStatusServiceImpl implements RafflesStatusService {
    private final RafflesPersistenceService rafflesPersistence;
    private final RafflesMapper mapper;

    @Override
    public RaffleDTO updateStatus(Long id, StatusUpdate request) {
        Raffle raffle = rafflesPersistence.findById(id);
        switch (request.status()) {
            case ACTIVE -> updateToActive(raffle);
            case PAUSED -> pause(raffle);
            case COMPLETED -> completeRaffle(raffle);
            case PENDING -> throw new BusinessException("Cannot revert to 'PENDING' state.");
            default -> throw new BusinessException("Unsupported status transition.");
        }
        return mapper.fromRaffle(rafflesPersistence.save(raffle));
    }

    @Override
    public void delete(Long id) {
        Raffle raffle = rafflesPersistence.findById(id);
        if (!raffle.getStatus().equals(RaffleStatus.PENDING)) {
            throw new BusinessException("Only raffles in 'PENDING' state can be deleted.");
        }
        rafflesPersistence.delete(raffle);
    }

    @Override
    public void completeRaffleIfAllTicketsSold(Raffle raffle) {
        boolean allTicketsSold = raffle.getTickets().stream().allMatch(ticket -> ticket.getStatus().equals(SOLD));
        if (allTicketsSold) {
            raffle.setStatus(COMPLETED);
            raffle.setCompletedAt(LocalDateTime.now());
            raffle.setCompletionReason(ALL_TICKETS_SOLD);
        }
        rafflesPersistence.save(raffle);
    }

    @Override
    public void updateStatusAfterAvailableTicketsIncrease(Raffle raffle) {
        if (raffle.getEndDate().isAfter(LocalDateTime.now()) &&
                raffle.getStatus().equals(COMPLETED) &&
                raffle.getCompletionReason().equals(ALL_TICKETS_SOLD)
        ) {
            raffle.setStatus(ACTIVE);
            raffle.setCompletedAt(null);
            raffle.setCompletionReason(null);
        } else if (raffle.getEndDate().isBefore(LocalDateTime.now())) {
            raffle.setCompletionReason(END_DATE_REACHED);
        }
        rafflesPersistence.save(raffle);
    }

    @Override
    public void reactivateRaffleAfterEndDateChange(Raffle raffle) {
        if (raffle.getStatus().equals(COMPLETED) && raffle.getCompletionReason().equals(CompletionReason.END_DATE_REACHED)) {
            try {
                reactivateRaffle(raffle);
            } catch (BusinessException ex) {}
        }
    }

    private void updateToActive(Raffle raffle) {
        switch (raffle.getStatus()) {
            case PENDING -> {
                validateRaffleEndDate(raffle);
                raffle.setStatus(ACTIVE);
                raffle.setStartDate(LocalDateTime.now());
            }
            case PAUSED -> {
                validateRaffleEndDate(raffle);
                raffle.setStatus(ACTIVE);
            }
            case COMPLETED -> reactivateRaffle(raffle);
            default -> throw new BusinessException("Invalid status transition to ACTIVE");
        }
    }

    private void pause(Raffle raffle) {
        if (!raffle.getStatus().equals(ACTIVE)) {
            throw new BusinessException("Only raffles in 'ACTIVE' state can be paused.");
        }
        raffle.setStatus(PAUSED);
    }

    private void completeRaffle(Raffle raffle) {
        RaffleStatus status = raffle.getStatus();
        if (!(status.equals(ACTIVE) || status.equals(PAUSED))) {
            throw new BusinessException("Cannot complete raffle unless it is active or paused");
        }
        raffle.setCompletionReason(MANUALLY_COMPLETED);
        raffle.setCompletedAt(LocalDateTime.now());
        raffle.setStatus(COMPLETED);
    }

    private void reactivateRaffle(Raffle raffle) {
        if (Objects.nonNull(raffle.getWinningTicket())) {
            throw new BusinessException("Cannot reactivate a raffle that already has a winner");
        }

        validateRaffleEndDate(raffle);

        if (raffle.getStatistics().getAvailableTickets() == 0 || raffle.getTotalTickets() <= raffle.getStatistics().getSoldTickets()) {
            throw new BusinessException("Available tickets for raffle are required to reactivate");
        }

        raffle.setStatus(ACTIVE);
        raffle.setCompletedAt(null);
        raffle.setCompletionReason(null);
    }

    private void validateRaffleEndDate(Raffle raffle) {
        if (raffle.getEndDate().isBefore(LocalDateTime.now().plusHours(24))) {
            throw new BusinessException("The end date of the raffle must be at least one day after the current date to reactivate");
        }
    }
}