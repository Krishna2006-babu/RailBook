package com.railbook.service;

import com.railbook.dto.BookingResponse;
import com.railbook.entity.AppUser;
import com.railbook.entity.Booking;
import com.railbook.entity.BookingStatus;
import com.railbook.entity.SeatInventory;
import com.railbook.entity.SeatStatus;
import com.railbook.repository.AppUserRepository;
import com.railbook.repository.BookingRepository;
import com.railbook.repository.SeatInventoryRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;

@Service
public class BookingService {

    private final BookingRepository bookingRepository;
    private final AppUserRepository appUserRepository;
    private final SeatInventoryRepository inventoryRepository;

    public BookingService(
            BookingRepository bookingRepository,
            AppUserRepository appUserRepository,
            SeatInventoryRepository inventoryRepository
    ) {
        this.bookingRepository = bookingRepository;
        this.appUserRepository = appUserRepository;
        this.inventoryRepository = inventoryRepository;
    }

    @Transactional(readOnly = true)
    public BookingResponse getBookingByPnr(String pnr, String email) {
        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);

        Booking booking = bookingRepository.findByPnr(pnr)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Booking not found"
                ));

        if (booking.getUser() == null ||
                !booking.getUser().getEmail().equalsIgnoreCase(normalizedEmail)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Booking not found"
            );
        }

        return toResponse(booking);
    }

    @Transactional(readOnly = true)
    public List<BookingResponse> getMyBookings(String email) {
        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);

        AppUser user = appUserRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Authenticated user not found"
                ));

        return bookingRepository.findByUserIdOrderByBookedAtDesc(user.getId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public BookingResponse cancelBooking(String pnr, String email) {
        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);

        Booking booking = bookingRepository
                .findByPnrAndUserEmailForUpdate(pnr, normalizedEmail)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Booking not found"
                ));

        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Booking is already cancelled"
            );
        }

        SeatInventory inventory = inventoryRepository
                .findForUpdate(
                        booking.getTrip().getId(),
                        booking.getSeat().getId()
                )
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Seat inventory not found"
                ));

        if (inventory.getStatus() != SeatStatus.BOOKED) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Seat inventory status is inconsistent with the booking"
            );
        }

        booking.setStatus(BookingStatus.CANCELLED);
        inventory.clearHold();

        bookingRepository.save(booking);
        inventoryRepository.save(inventory);

        return toResponse(booking);
    }

    private BookingResponse toResponse(Booking booking) {
        return new BookingResponse(
                booking.getPnr(),
                booking.getTrip().getId(),
                booking.getTrip().getTrain().getId(),
                booking.getTrip().getTrain().getName(),
                booking.getSeat().getId(),
                booking.getSeat().getCoachNumber(),
                booking.getSeat().getSeatNumber(),
                booking.getSeat().getSeatClass(),
                booking.getStatus().name(),
                booking.getBookedAt()
        );
    }
}