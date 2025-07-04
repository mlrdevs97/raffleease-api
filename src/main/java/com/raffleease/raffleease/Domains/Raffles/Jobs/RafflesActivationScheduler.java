package com.raffleease.raffleease.Domains.Raffles.Jobs;

import com.raffleease.raffleease.Domains.Raffles.Model.Raffle;
import com.raffleease.raffleease.Domains.Raffles.Model.RaffleStatus;
import com.raffleease.raffleease.Domains.Raffles.Repository.RafflesRepository;
import com.raffleease.raffleease.Domains.Raffles.Services.RafflesPersistenceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.raffleease.raffleease.Domains.Raffles.Model.RaffleStatus.ACTIVE;
import static com.raffleease.raffleease.Domains.Raffles.Model.RaffleStatus.PENDING;

@Slf4j
@RequiredArgsConstructor
@Service
public class RafflesActivationScheduler {
    private final RafflesPersistenceService persistenceService;
    private final RafflesRepository repository;

    @Scheduled(cron = "${spring.application.configs.cron.raffles_activation}")
    @Transactional
    public void activatePendingRaffles() {
        log.info("Starting scheduled activation of pending raffles");
        
        List<Raffle> pendingRaffles = repository.findAllPendingRafflesToActivate(PENDING);
        
        if (pendingRaffles.isEmpty()) {
            log.debug("No pending raffles found for activation");
            return;
        }
        
        log.info("Found {} pending raffles to activate", pendingRaffles.size());
        
        for (Raffle raffle : pendingRaffles) {
            raffle.setStatus(ACTIVE);
            log.debug("Activating raffle with ID: {}", raffle.getId());
        }
        
        persistenceService.saveAll(pendingRaffles);
        log.info("Successfully activated {} pending raffles", pendingRaffles.size());
    }
} 