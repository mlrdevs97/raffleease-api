package com.raffleease.raffleease.Domains.Raffles.Services.Impl;

import com.raffleease.raffleease.Domains.Associations.Model.Association;
import com.raffleease.raffleease.Domains.Associations.Services.AssociationsService;
import com.raffleease.raffleease.Domains.Images.Model.Image;
import com.raffleease.raffleease.Domains.Images.Services.ImagesAssociateService;
import com.raffleease.raffleease.Domains.Raffles.DTOs.RaffleCreate;
import com.raffleease.raffleease.Domains.Raffles.DTOs.RaffleDTO;
import com.raffleease.raffleease.Domains.Raffles.Mappers.RafflesMapper;
import com.raffleease.raffleease.Domains.Raffles.Model.Raffle;
import com.raffleease.raffleease.Domains.Raffles.Model.RaffleStatistics;
import com.raffleease.raffleease.Domains.Raffles.Services.RafflesCreateService;
import com.raffleease.raffleease.Domains.Raffles.Services.RafflesPersistenceService;
import com.raffleease.raffleease.Domains.Tickets.Model.Ticket;
import com.raffleease.raffleease.Domains.Tickets.Services.TicketsService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

import static com.raffleease.raffleease.Domains.Raffles.Model.RaffleStatus.PENDING;
import static java.math.BigDecimal.ZERO;

@RequiredArgsConstructor
@Service
public class RafflesCreateServiceImpl implements RafflesCreateService {
    private final RafflesPersistenceService rafflesPersistence;
    private final TicketsService ticketsService;
    private final RafflesMapper rafflesMapper;
    private final AssociationsService associationsService;
    private final ImagesAssociateService imagesAssociateService;

    @Override
    @Transactional
    public RaffleDTO create(Long associationId, RaffleCreate raffleData) {
        // 1. Create new raffle
        Association association = associationsService.findById(associationId);
        Raffle newRaffle = createNewRaffle(raffleData, association);
        // 2. Create statistics
        RaffleStatistics statistics = createNewStatistics(newRaffle, raffleData.ticketsInfo().amount());
        newRaffle.setStatistics(statistics);
        // 3. Associate images without paths/URLs
        List<Image> images = imagesAssociateService.associateImagesToRaffleOnCreate(newRaffle, raffleData.images());
        newRaffle.getImages().addAll(images);
        // 4. Create tickets
        List<Ticket> tickets = ticketsService.create(newRaffle, raffleData.ticketsInfo());
        newRaffle.getTickets().addAll(tickets);
        // 5. Save the raffle
        Raffle savedRaffle = rafflesPersistence.save(newRaffle);
        // 6. Finalize the image paths and URLs with the saved raffle's ID
        imagesAssociateService.finalizeImagePathsAndUrls(savedRaffle, images);

        return rafflesMapper.fromRaffle(savedRaffle);
    }

    private Raffle createNewRaffle(RaffleCreate raffleData, Association association) {
        return Raffle.builder()
                .association(association)
                .title(raffleData.title())
                .description(raffleData.description())
                .status(PENDING)
                .ticketPrice(raffleData.ticketsInfo().price())
                .totalTickets(raffleData.ticketsInfo().amount())
                .firstTicketNumber(raffleData.ticketsInfo().lowerLimit())
                .images(new ArrayList<>())
                .tickets(new ArrayList<>())
                .orders(new ArrayList<>())
                .startDate(raffleData.startDate())
                .endDate(raffleData.endDate())
                .build();
    }

    private RaffleStatistics createNewStatistics(Raffle raffle, long ticketAmount) {
        return RaffleStatistics.builder()
                .raffle(raffle)
                .availableTickets(ticketAmount)
                .soldTickets(0L)
                .revenue(ZERO)
                .averageOrderValue(ZERO)
                .totalOrders(0L)
                .completedOrders(0L)
                .pendingOrders(0L)
                .cancelledOrders(0L)
                .unpaidOrders(0L)
                .refundedOrders(0L)
                .participants(0L)
                .ticketsPerParticipant(ZERO)
                .firstSaleDate(null)
                .lastSaleDate(null)
                .dailySalesVelocity(ZERO)
                .build();
    }
}
