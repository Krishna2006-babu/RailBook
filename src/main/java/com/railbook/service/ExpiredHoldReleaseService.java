package com.railbook.service;

import com.railbook.entity.SeatInventory;
import com.railbook.entity.SeatStatus;
import com.railbook.repository.SeatInventoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class ExpiredHoldReleaseService {

    private final SeatInventoryRepository inventoryRepository;

    public ExpiredHoldReleaseService(SeatInventoryRepository inventoryRepository) {
        this.inventoryRepository = inventoryRepository;
    }

    @Transactional
    public boolean releaseIfExpired(Long inventoryId) {
        SeatInventory inventory = inventoryRepository.findByIdForUpdate(inventoryId)
                .orElse(null);

        if (inventory == null) {
            return false;
        }

        if (inventory.getStatus() == SeatStatus.HELD
                && inventory.getHoldExpiresAt() != null
                && !inventory.getHoldExpiresAt().isAfter(Instant.now())) {

            inventory.clearHold();
            return true;
        }

        return false;
    }
}