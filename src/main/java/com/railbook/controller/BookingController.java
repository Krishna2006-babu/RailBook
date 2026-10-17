
package com.railbook.controller;

import com.railbook.dto.BookingResponse;
import com.railbook.service.BookingService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @GetMapping("/{pnr}")
    public BookingResponse getBookingByPnr(
            @PathVariable String pnr
    ) {
        return bookingService.getBookingByPnr(pnr);
    }

    @GetMapping("/trip/{tripId}")
    public List<BookingResponse> getBookingsByTripId(
            @PathVariable Long tripId
    ) {
        return bookingService.getBookingsByTripId(tripId);
    }

    @GetMapping("/my")
    public List<BookingResponse> getMyBookings(
            Authentication authentication
    ) {
        return bookingService.getMyBookings(authentication.getName());
    }
}
