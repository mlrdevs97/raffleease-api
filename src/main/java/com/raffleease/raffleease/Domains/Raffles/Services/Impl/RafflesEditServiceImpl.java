package com.raffleease.raffleease.Domains.Raffles.Services.Impl;

import com.raffleease.raffleease.Domains.Images.DTOs.ImageDTO;
import com.raffleease.raffleease.Domains.Images.Model.Image;
import com.raffleease.raffleease.Domains.Images.Services.ImagesAssociateService;
import com.raffleease.raffleease.Domains.Raffles.DTOs.RaffleDTO;
import com.raffleease.raffleease.Domains.Raffles.DTOs.RaffleEdit;
import com.raffleease.raffleease.Domains.Raffles.Mappers.RafflesMapper;
import com.raffleease.raffleease.Domains.Raffles.Model.Raffle;
import com.raffleease.raffleease.Domains.Raffles.Model.RaffleStatistics;
import com.raffleease.raffleease.Domains.Raffles.Services.RafflesEditService;
import com.raffleease.raffleease.Domains.Raffles.Services.RafflesPersistenceService;
import com.raffleease.raffleease.Domains.Raffles.Services.RafflesStatusService;
import com.raffleease.raffleease.Domains.Tickets.DTO.TicketsCreate;
import com.raffleease.raffleease.Domains.Tickets.Model.Ticket;
import com.raffleease.raffleease.Domains.Tickets.Services.TicketsService;
import com.raffleease.raffleease.Common.Exceptions.CustomExceptions.BusinessException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

import static com.raffleease.raffleease.Domains.Raffles.Model.RaffleStatus.ACTIVE;
import static com.raffleease.raffleease.Domains.Raffles.Model.RaffleStatus.COMPLETED;

@RequiredArgsConstructor
@Service
public class RafflesEditServiceImpl implements RafflesEditService {
    private final RafflesPersistenceService rafflesPersistence;
    private final RafflesStatusService rafflesStatusService;
    private final TicketsService ticketsCreateService;
    private final ImagesAssociateService imagesAssociateService;
    private final RafflesMapper rafflesMapper;

    @Transactional
    public RaffleDTO edit(Long id, RaffleEdit raffleEdit) {
        Raffle raffle = rafflesPersistence.findById(id);

        if (raffleEdit.title() != null) {
            raffle.setTitle(raffleEdit.title());
        }

        if (raffleEdit.description() != null) {
            raffle.setDescription(raffleEdit.description());
        }

        if (raffleEdit.endDate() != null) {
            editEndDate(raffle, raffleEdit.endDate());
        }

        if (raffleEdit.images() != null && !raffleEdit.images().isEmpty()) {
            addNewImages(raffle, raffleEdit.images());
        }

        if (raffleEdit.ticketPrice() != null) {
            raffle.setTicketPrice(raffleEdit.ticketPrice());
        }

        if (raffleEdit.totalTickets() != null) {
            editTotalTickets(raffle, raffleEdit.totalTickets());
        }

        raffle.setUpdatedAt(LocalDateTime.now());
        Raffle savedRaffle = rafflesPersistence.save(raffle);
        return rafflesMapper.fromRaffle(savedRaffle);
    }

    private void editEndDate(Raffle raffle, LocalDateTime endDate) {
        if (raffle.getStartDate() != null && endDate.isBefore(raffle.getStartDate().plusHours(24))) {
            throw new BusinessException("The end date of the raffle must be at least one day after the start date");
        }
        raffle.setEndDate(endDate);
        rafflesStatusService.reactivateRaffleAfterEndDateChange(raffle);
    }

    private void addNewImages(Raffle raffle, List<ImageDTO> imageDTOs) {
        List<Image> images = imagesAssociateService.associateImagesToRaffleOnEdit(raffle, imageDTOs);
        raffle.getImages().clear();
        raffle.getImages().addAll(images);
    }

    private void editTotalTickets(Raffle raffle, long editTotal) {
        RaffleStatistics statistics = raffle.getStatistics();

        if (statistics.getSoldTickets() != null && editTotal < statistics.getSoldTickets()) {
            throw new BusinessException("The total tickets count cannot be less than the number of tickets already sold for this raffle");
        }

        long oldTotal = raffle.getTotalTickets();
        raffle.setTotalTickets(editTotal);

        long ticketDifference = editTotal - oldTotal;
        if (ticketDifference <= 0) {
            return;
        }

        statistics.setAvailableTickets(statistics.getAvailableTickets() + ticketDifference);
        createAdditionalTickets(raffle, oldTotal, ticketDifference);
        rafflesStatusService.updateStatusAfterAvailableTicketsIncrease(raffle);
    }

    private void createAdditionalTickets(Raffle raffle, long oldTotal, long amount) {
        long lowerLimit = raffle.getFirstTicketNumber() + oldTotal;

        TicketsCreate request = TicketsCreate.builder()
                .amount(amount)
                .price(raffle.getTicketPrice())
                .lowerLimit(lowerLimit)
                .build();

        List<Ticket> newTickets = ticketsCreateService.create(raffle, request);
        raffle.getTickets().addAll(newTickets);
    }
}