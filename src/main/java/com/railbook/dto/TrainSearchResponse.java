package com.railbook.dto;

import java.math.BigDecimal;
import java.time.LocalTime;

public record TrainSearchResponse(
        String trainNumber,
        String trainName,
        String sourceCode,
        String sourceName,
        LocalTime departureTime,
        String destinationCode,
        String destinationName,
        LocalTime arrivalTime,
        BigDecimal fare
) {
}