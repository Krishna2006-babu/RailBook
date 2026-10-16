package com.railbook.controller;

import com.railbook.dto.SeatHoldResponse;
import com.railbook.service.SeatHoldService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/trips")
public class SeatHoldController {

    private final SeatHoldService seatHoldService;

    public SeatHoldController(SeatHoldService seatHoldService) {
        this.seatHoldService = seatHoldService;
    }

    @PostMapping("/{tripId}/seats/{seatId}/hold")
    public SeatHoldResponse holdSeat(
            @PathVariable Long tripId,
            @PathVariable Long seatId) {

        return seatHoldService.holdSeat(tripId, seatId);
    }
}