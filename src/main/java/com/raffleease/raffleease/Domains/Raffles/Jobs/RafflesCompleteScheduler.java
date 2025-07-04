package com.raffleease.raffleease.Domains.Raffles.Jobs;

import com.raffleease.raffleease.Domains.Raffles.Model.Raffle;
import com.raffleease.raffleease.Domains.Raffles.Repository.RafflesRepository;
import com.raffleease.raffleease.Domains.Raffles.Services.RafflesPersistenceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static com.raffleease.raffleease.Domains.Raffles.Model.CompletionReason.END_DATE_REACHED;
import static com.raffleease.raffleease.Domains.Raffles.Model.RaffleStatus.ACTIVE;
import static com.raffleease.raffleease.Domains.Raffles.Model.RaffleStatus.COMPLETED;

@Slf4j
@RequiredArgsConstructor
@Service
public class RafflesCompleteScheduler {
    private final RafflesPersistenceService persistenceService;
    private final RafflesRepository repository;

    @Scheduled(cron = "${spring.application.configs.cron.raffles_completion}")
    @Transactional
    public void completeRaffles() {
        log.info("Starting scheduled completion of active raffles");
        
        List<Raffle> raffles = repository.findAllEligibleForCompletion(ACTIVE);
        
        if (raffles.isEmpty()) {
            log.debug("No active raffles found for completion");
            return;
        }
        
        log.info("Found {} active raffles to complete", raffles.size());
        
        for (Raffle raffle : raffles) {
            raffle.setStatus(COMPLETED);
            raffle.setCompletionReason(END_DATE_REACHED);
            raffle.setCompletedAt(LocalDateTime.now());
            log.debug("Completing raffle with ID: {}", raffle.getId());
        }
        
        persistenceService.saveAll(raffles);
        log.info("Successfully completed {} raffles", raffles.size());
    }
}
