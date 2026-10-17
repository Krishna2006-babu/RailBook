
package com.railbook.service;

import com.railbook.dto.SeatHoldResponse;
import com.railbook.dto.SeatConfirmationResponse;
import com.railbook.entity.Booking;
import com.railbook.entity.SeatInventory;
import com.railbook.entity.SeatStatus;
import com.railbook.repository.SeatInventoryRepository;
import com.railbook.repository.BookingRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.UUID;

@Service
public class SeatHoldService {

    private final SeatInventoryRepository inventoryRepository;
    private final BookingRepository bookingRepository;
    private final PnrGenerator pnrGenerator;

    public SeatHoldService(
            SeatInventoryRepository inventoryRepository,
            BookingRepository bookingRepository,
            PnrGenerator pnrGenerator
    ) {
        this.inventoryRepository = inventoryRepository;
        this.bookingRepository = bookingRepository;
        this.pnrGenerator = pnrGenerator;
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

    @Transactional
    public SeatConfirmationResponse confirmSeat(
            Long tripId,
            Long seatId,
            String holdToken
    ) {
        SeatInventory inventory = inventoryRepository
                .findForUpdate(tripId, seatId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Seat inventory not found"
                ));

        if (inventory.getStatus() != SeatStatus.HELD) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Seat is not currently held"
            );
        }

        if (holdToken == null
                || holdToken.isBlank()
                || !holdToken.equals(inventory.getHoldToken())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Invalid hold token"
            );
        }

        Instant now = Instant.now();

        if (inventory.getHoldExpiresAt() == null
                || !inventory.getHoldExpiresAt().isAfter(now)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Seat hold has expired"
            );
        }

        String pnr = generateUniquePnr();

        Booking booking = new Booking(
                pnr,
                inventory.getTrip(),
                inventory.getSeat(),
                now
        );

        // Persist booking within the current transaction.
        bookingRepository.saveAndFlush(booking);

        // The booking record and inventory update commit together.
        inventory.confirmBooking();

        return new SeatConfirmationResponse(
                tripId,
                seatId,
                SeatStatus.BOOKED.name(),
                pnr,
                now
        );
    }

    private String generateUniquePnr() {
        for (int attempt = 0; attempt < 5; attempt++) {
            String pnr = pnrGenerator.generate();

            if (!bookingRepository.existsByPnr(pnr)) {
                return pnr;
            }
        }

        throw new ResponseStatusException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Could not generate a unique PNR"
        );
    }
}
