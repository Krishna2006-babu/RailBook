
package com.railbook.controller;

import com.railbook.dto.SeatHoldResponse;
import com.railbook.dto.SeatConfirmationResponse;
import com.railbook.dto.ConfirmSeatHoldRequest;
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

    @PostMapping("/{tripId}/seats/{seatId}/confirm")
    public SeatConfirmationResponse confirmSeat(
            @PathVariable Long tripId,
            @PathVariable Long seatId,
            @RequestBody ConfirmSeatHoldRequest request) {

        return seatHoldService.confirmSeat(
                tripId,
                seatId,
                request.holdToken()
        );
    }
}
