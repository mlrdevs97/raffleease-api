package com.raffleease.raffleease.Domains.Raffles.Mappers.Impls;

import com.raffleease.raffleease.Domains.Images.DTOs.ImageDTO;
import com.raffleease.raffleease.Domains.Images.Mappers.ImagesMapper;
import com.raffleease.raffleease.Domains.Raffles.DTOs.RaffleDTO;
import com.raffleease.raffleease.Domains.Raffles.DTOs.RaffleStatisticsDTO;
import com.raffleease.raffleease.Domains.Raffles.Mappers.RafflesMapper;
import com.raffleease.raffleease.Domains.Raffles.Model.Raffle;
import com.raffleease.raffleease.Domains.Raffles.Model.RaffleStatistics;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

@RequiredArgsConstructor
@Service
public class RafflesMapperImpl implements RafflesMapper {
    private final ImagesMapper imagesMapper;

    @Override
    public RaffleDTO fromRaffle(Raffle raffle) {
        List<ImageDTO> images = imagesMapper.fromImagesList(raffle.getImages()).stream()
                .sorted(Comparator.comparing(ImageDTO::imageOrder))
                .toList();

        return RaffleDTO.builder()
                .id(raffle.getId())
                .associationId(raffle.getAssociation().getId())
                .title(raffle.getTitle())
                .description(raffle.getDescription())
                .status(raffle.getStatus())
                .ticketPrice(raffle.getTicketPrice())
                .totalTickets(raffle.getTotalTickets())
                .firstTicketNumber(raffle.getFirstTicketNumber())
                .images(images)
                .completionReason(raffle.getCompletionReason())
                .winningTicketId(raffle.getWinningTicket() != null ? raffle.getWinningTicket().getId() : null)
                .statistics(fromStatistics(raffle.getId(), raffle.getStatistics()))
                .startDate(raffle.getStartDate())
                .endDate(raffle.getEndDate())
                .createdAt(raffle.getCreatedAt())
                .updatedAt(raffle.getUpdatedAt())
                .completedAt(raffle.getCompletedAt())
                .build();
    }

    @Override
    public List<RaffleDTO> fromRaffleList(List<Raffle> raffles) {
        return raffles.stream().map(this::fromRaffle).toList();
    }

    private RaffleStatisticsDTO fromStatistics(Long raffleId, RaffleStatistics statistics) {
        return RaffleStatisticsDTO.builder()
                .id(statistics.getId())
                .raffleId(raffleId)
                .availableTickets(statistics.getAvailableTickets())
                .soldTickets(statistics.getSoldTickets())
                .revenue(statistics.getRevenue())
                .averageOrderValue(statistics.getAverageOrderValue())
                .totalOrders(statistics.getTotalOrders())
                .completedOrders(statistics.getCompletedOrders())
                .pendingOrders(statistics.getPendingOrders())
                .cancelledOrders(statistics.getCancelledOrders())
                .unpaidOrders(statistics.getUnpaidOrders())
                .refundedOrders(statistics.getRefundedOrders())
                .participants(statistics.getParticipants())
                .ticketsPerParticipant(statistics.getTicketsPerParticipant())
                .firstSaleDate(statistics.getFirstSaleDate())
                .lastSaleDate(statistics.getLastSaleDate())
                .dailySalesVelocity(statistics.getDailySalesVelocity())
                .build();
    }
}