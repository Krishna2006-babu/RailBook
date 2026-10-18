package com.railbook.service;

import com.railbook.repository.SeatInventoryRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@Component
public class ExpiredHoldCleanupScheduler {

    private final SeatInventoryRepository inventoryRepository;
    private final ExpiredHoldReleaseService releaseService;

    public ExpiredHoldCleanupScheduler(
            SeatInventoryRepository inventoryRepository,
            ExpiredHoldReleaseService releaseService) {
        this.inventoryRepository = inventoryRepository;
        this.releaseService = releaseService;
    }

    @Scheduled(fixedDelay = 60000)
    public void cleanupExpiredHolds() {
        List<Long> expiredIds =
                inventoryRepository.findExpiredHoldIds(Instant.now());

        for (Long inventoryId : expiredIds) {
            releaseService.releaseIfExpired(inventoryId);
        }
        System.out.println("Expired hold cleanup running at " + Instant.now());
    }
}