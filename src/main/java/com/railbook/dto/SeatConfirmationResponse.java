
package com.railbook.dto;

import java.time.Instant;

public record SeatConfirmationResponse(
        Long tripId,
        Long seatId,
        String status,
        String pnr,
        Instant confirmedAt
) {
}
