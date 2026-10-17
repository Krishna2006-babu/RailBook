
package com.railbook.dto;

import java.time.Instant;

public record BookingResponse(
        String pnr,
        Long tripId,
        Long trainId,
        String trainName,
        Long seatId,
        String coachNumber,
        String seatNumber,
        String seatClass,
        String status,
        Instant bookedAt
) {}
