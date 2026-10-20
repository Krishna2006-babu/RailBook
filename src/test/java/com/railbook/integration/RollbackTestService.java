package com.railbook.integration;

import com.railbook.entity.Booking;
import com.railbook.entity.SeatInventory;
import com.railbook.repository.BookingRepository;
import com.railbook.repository.SeatInventoryRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RollbackTestService {

    private final BookingRepository bookingRepository;
    private final SeatInventoryRepository inventoryRepository;

    public RollbackTestService(
            BookingRepository bookingRepository,
            SeatInventoryRepository inventoryRepository
    ) {
        this.bookingRepository = bookingRepository;
        this.inventoryRepository = inventoryRepository;
    }

    @Transactional
    public void saveBookingAndFail(
            Booking booking,
            Long inventoryId
    ) {
        bookingRepository.saveAndFlush(booking);

        SeatInventory inventory = inventoryRepository
                .findByIdForUpdate(inventoryId)
                .orElseThrow();

        inventory.confirmBooking();

        inventoryRepository.flush();

        throw new IllegalStateException("Simulated transaction failure");
    }
}