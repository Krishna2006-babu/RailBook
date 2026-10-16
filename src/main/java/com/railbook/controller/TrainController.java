package com.railbook.controller;

import com.railbook.dto.TrainSearchResponse;
import com.railbook.service.TrainService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/trains")
public class TrainController {

    private final TrainService trainService;

    public TrainController(TrainService trainService) {
        this.trainService = trainService;
    }

    @GetMapping("/search")
    public List<TrainSearchResponse> searchTrains(
            @RequestParam String from,
            @RequestParam String to) {

        return trainService.searchTrains(from, to);
    }
}