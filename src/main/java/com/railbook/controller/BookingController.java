
package com.railbook.controller;

import com.railbook.dto.BookingResponse;
import com.railbook.service.BookingService;
import org.springframework.web.bind.annotation.*;

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
    public java.util.List<BookingResponse> getBookingsByTripId(
            @PathVariable Long tripId
    ) {
        return bookingService.getBookingsByTripId(tripId);
    }

}
