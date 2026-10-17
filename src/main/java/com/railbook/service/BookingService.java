
package com.railbook.service;

import com.railbook.dto.BookingResponse;
import com.railbook.entity.AppUser;
import com.railbook.entity.Booking;
import com.railbook.repository.AppUserRepository;
import com.railbook.repository.BookingRepository;
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

    public BookingService(
            BookingRepository bookingRepository,
            AppUserRepository appUserRepository
    ) {
        this.bookingRepository = bookingRepository;
        this.appUserRepository = appUserRepository;
    }

    @Transactional(readOnly = true)
    public BookingResponse getBookingByPnr(String pnr) {
        Booking booking = bookingRepository.findByPnr(pnr)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Booking not found for PNR: " + pnr
                ));

        return toResponse(booking);
    }

    @Transactional(readOnly = true)
    public List<BookingResponse> getBookingsByTripId(Long tripId) {
        return bookingRepository.findByTripIdOrderByBookedAtDesc(tripId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<BookingResponse> getMyBookings(String email) {
        String normalizedEmail = email.trim()
                .toLowerCase(Locale.ROOT);

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
                "BOOKED",
                booking.getBookedAt()
        );
    }
}
