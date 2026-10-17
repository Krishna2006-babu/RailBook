
package com.railbook.service;

import com.railbook.dto.BookingResponse;
import com.railbook.entity.Booking;
import com.railbook.repository.BookingRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class BookingService {

    private final BookingRepository bookingRepository;

    public BookingService(BookingRepository bookingRepository) {
        this.bookingRepository = bookingRepository;
    }

    @Transactional(readOnly = true)
    public BookingResponse getBookingByPnr(String pnr) {
        Booking booking = bookingRepository.findByPnr(pnr)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Booking not found for PNR: " + pnr
                ));

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
    @Transactional(readOnly = true)
    public java.util.List<BookingResponse> getBookingsByTripId(Long tripId) {
        return bookingRepository.findByTripIdOrderByBookedAtDesc(tripId)
                .stream()
                .map(booking -> new BookingResponse(
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
                ))
                .toList();
    }
}
