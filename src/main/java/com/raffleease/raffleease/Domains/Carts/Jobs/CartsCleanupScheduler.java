package com.raffleease.raffleease.Domains.Carts.Jobs;

import com.raffleease.raffleease.Domains.Carts.Model.Cart;
import com.raffleease.raffleease.Domains.Carts.Repository.CartsRepository;
import com.raffleease.raffleease.Domains.Carts.Services.CartLifecycleService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Service
public class CartsCleanupScheduler {
    private final CartsRepository cartsRepository;
    private final CartLifecycleService cartLifecycleService;

    @Value("${spring.application.configs.cleanup.carts_cleanup_cutoff_seconds}")
    private Long cutoffSeconds;

    @Scheduled(cron = "${spring.application.configs.cron.carts_cleanup}")
    @Transactional
    public void releaseScheduled() {
        log.info("Starting scheduled cart cleanup process");
        
        LocalDateTime updatedAt = LocalDateTime.now().minusSeconds(cutoffSeconds);
        List<Cart> expiredCarts = cartsRepository.findAllByUpdatedAtBefore(updatedAt);
        
        if (expiredCarts.isEmpty()) {
            log.info("No expired carts found for cleanup");
            return;
        }
        
        log.info("Found {} expired carts for cleanup", expiredCarts.size());
        cartLifecycleService.releaseExpiredCarts(expiredCarts);
        log.info("Completed scheduled cart cleanup process");
    }
}
