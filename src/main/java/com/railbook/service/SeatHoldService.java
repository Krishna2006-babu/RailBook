package com.railbook.service;

import com.railbook.dto.SeatHoldResponse;
import com.railbook.entity.SeatInventory;
import com.railbook.entity.SeatStatus;
import com.railbook.repository.SeatInventoryRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.UUID;

@Service
public class SeatHoldService {

    private final SeatInventoryRepository inventoryRepository;

    public SeatHoldService(
            SeatInventoryRepository inventoryRepository) {
        this.inventoryRepository = inventoryRepository;
    }

    @Transactional
    public SeatHoldResponse holdSeat(Long tripId, Long seatId) {

        SeatInventory inventory = inventoryRepository
                .findForUpdate(tripId, seatId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Seat inventory not found"
                ));

        Instant now = Instant.now();

        if (inventory.getStatus() == SeatStatus.BOOKED) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Seat is already booked"
            );
        }

        if (inventory.getStatus() == SeatStatus.HELD
                && inventory.getHoldExpiresAt() != null
                && inventory.getHoldExpiresAt().isAfter(now)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Seat is currently held by another booking"
            );
        }

        String token = UUID.randomUUID().toString();
        Instant expiresAt = now.plusSeconds(300);

        inventory.hold(token, expiresAt);

        return new SeatHoldResponse(
                tripId,
                seatId,
                SeatStatus.HELD.name(),
                token,
                expiresAt
        );
    }
}