package com.raffleease.raffleease.Domains.Raffles.Services.Impl;

import com.raffleease.raffleease.Common.Exceptions.CustomExceptions.BusinessException;
import com.raffleease.raffleease.Domains.Raffles.Model.Raffle;
import com.raffleease.raffleease.Domains.Raffles.Model.RaffleStatistics;
import com.raffleease.raffleease.Domains.Raffles.Services.RafflesStatisticsService;
import com.raffleease.raffleease.Domains.Raffles.Services.RafflesPersistenceService;
import com.raffleease.raffleease.Domains.Tickets.Model.Ticket;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.math.BigDecimal.ZERO;
import static java.math.RoundingMode.HALF_UP;

@RequiredArgsConstructor
@Service
public class RafflesStatisticsServiceImpl implements RafflesStatisticsService {
    private final RafflesPersistenceService rafflesPersistence;

    @Override
    public void setReservationStatistics(Raffle raffle, long reductionQuantity) {
        RaffleStatistics statistics = raffle.getStatistics();
        long availableTickets = statistics.getAvailableTickets() - reductionQuantity;
        if (availableTickets < 0) {
            throw new BusinessException("Insufficient tickets available to complete the operation");
        }
        statistics.setAvailableTickets(availableTickets);
        statistics.setParticipants(statistics.getParticipants() + 1);
        updateTicketsPerParticipant(statistics, raffle.getTotalTickets());
        rafflesPersistence.save(raffle);
    }

    @Override
    public void setReleaseStatistics(Raffle raffle, long increaseQuantity) {
        RaffleStatistics statistics = raffle.getStatistics();
        Long availableTickets = statistics.getAvailableTickets() + increaseQuantity;
        if (availableTickets > raffle.getTotalTickets()) {
            throw new BusinessException("The operation exceeds the total ticket limit");
        }
        statistics.setAvailableTickets(availableTickets);
        statistics.setParticipants(statistics.getParticipants() - 1);
        updateTicketsPerParticipant(statistics, raffle.getTotalTickets());
        rafflesPersistence.save(raffle);
    }

    @Override
    public void setCreateOrderStatistics(Raffle raffle, long reservedTickets) {
        RaffleStatistics statistics = raffle.getStatistics();
        statistics.setTotalOrders(statistics.getTotalOrders() + 1);
        statistics.setPendingOrders(statistics.getPendingOrders() + 1);
        rafflesPersistence.save(raffle);
    }

    @Override
    public void setCancelStatistics(Raffle raffle, long cancelledTickets) {
        RaffleStatistics statistics = raffle.getStatistics();
        statistics.setPendingOrders(statistics.getPendingOrders() - 1);
        statistics.setCancelledOrders(statistics.getCancelledOrders() + 1);
        statistics.setAvailableTickets(statistics.getAvailableTickets() + cancelledTickets);
        rafflesPersistence.save(raffle);
    }

    @Override
    public void setCompleteStatistics(Raffle raffle, long soldTickets) {
        RaffleStatistics statistics = raffle.getStatistics();
        statistics.setPendingOrders(statistics.getPendingOrders() - 1);
        statistics.setSoldTickets(statistics.getSoldTickets() + soldTickets);
        long newCompletedOrders = statistics.getCompletedOrders() + 1;
        statistics.setCompletedOrders(newCompletedOrders);
        BigDecimal sellAmount = calculateAmount(raffle.getTicketPrice(), soldTickets);
        BigDecimal newRevenue = statistics.getRevenue().add(sellAmount);
        statistics.setRevenue(newRevenue);
        if (newCompletedOrders > 0) {
            statistics.setAverageOrderValue(newRevenue.divide(BigDecimal.valueOf(newCompletedOrders), 2, HALF_UP));
        }
        if (statistics.getFirstSaleDate() == null) {
            statistics.setFirstSaleDate(LocalDateTime.now());
        }
        statistics.setLastSaleDate(LocalDateTime.now());
        rafflesPersistence.save(raffle);
    }

    @Override
    public void setRefundStatistics(Raffle raffle, long refundTickets) {
        RaffleStatistics statistics = raffle.getStatistics();
        statistics.setRefundedOrders(statistics.getRefundedOrders() + 1);
        long newCompletedOrders = statistics.getCompletedOrders() - 1;
        statistics.setCompletedOrders(newCompletedOrders);
        statistics.setSoldTickets(statistics.getSoldTickets() - refundTickets);
        statistics.setAvailableTickets(statistics.getAvailableTickets() + refundTickets);
        BigDecimal refundAmount = calculateAmount(raffle.getTicketPrice(), refundTickets);
        BigDecimal newRevenue = statistics.getRevenue().subtract(refundAmount);
        statistics.setRevenue(newRevenue);
        if (newCompletedOrders > 0) {
            statistics.setAverageOrderValue(newRevenue.divide(BigDecimal.valueOf(newCompletedOrders), 2, HALF_UP));
        } else {
            statistics.setAverageOrderValue(ZERO);
        }
        rafflesPersistence.save(raffle);
    }

    @Override
    public void setUnpaidStatistics(Raffle raffle, long unpaidTickets) {
        RaffleStatistics statistics = raffle.getStatistics();
        statistics.setPendingOrders(statistics.getPendingOrders() - 1);
        statistics.setUnpaidOrders(statistics.getUnpaidOrders() + 1);
        statistics.setAvailableTickets(statistics.getAvailableTickets() + unpaidTickets);
        rafflesPersistence.save(raffle);
    }

    @Override
    public void increaseRafflesTicketsAvailability(List<Ticket> tickets) {
        Map<Raffle, Long> ticketsByRaffle = tickets.stream()
                .collect(Collectors.groupingBy(Ticket::getRaffle, Collectors.counting()));
        ticketsByRaffle.forEach(this::setReleaseStatistics);
    }

    @Override
    public void reduceRaffleTicketsAvailability(List<Ticket> tickets) {
        Map<Raffle, Long> ticketsByRaffle = tickets.stream().collect(
                Collectors.groupingBy(Ticket::getRaffle, Collectors.counting())
        );
        ticketsByRaffle.forEach(this::setReservationStatistics);
    }


    private BigDecimal calculateAmount(BigDecimal ticketPrice, long numTickets) {
        return ticketPrice.multiply(BigDecimal.valueOf(numTickets));
    }

    private void updateTicketsPerParticipant(RaffleStatistics statistics, long totalTickets) {
        long unavailableTickets = totalTickets - statistics.getAvailableTickets();
        if (statistics.getParticipants() > 0) {
            BigDecimal preciseResult = new BigDecimal(unavailableTickets).divide(new BigDecimal(statistics.getParticipants()), 2, HALF_UP);
            statistics.setTicketsPerParticipant(preciseResult);
        } else {
            statistics.setTicketsPerParticipant(ZERO);
        }
    }
}
