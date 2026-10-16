package com.railbook.dto;

import java.time.Instant;

public record SeatHoldResponse(
        Long tripId,
        Long seatId,
        String status,
        String holdToken,
        Instant expiresAt
) {
}