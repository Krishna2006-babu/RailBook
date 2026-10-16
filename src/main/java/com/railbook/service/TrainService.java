package com.railbook.service;

import com.railbook.dto.TrainSearchResponse;
import com.railbook.entity.Train;
import com.railbook.entity.TrainStop;
import com.railbook.repository.TrainRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;

@Service
public class TrainService {

    private final TrainRepository trainRepository;

    public TrainService(TrainRepository trainRepository) {
        this.trainRepository = trainRepository;
    }

    @Transactional(readOnly = true)
    public List<TrainSearchResponse> searchTrains(
            String from, String to) {

        if (from == null || to == null ||
                from.isBlank() || to.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Source and destination are required"
            );
        }

        final String sourceCode =
                from.trim().toUpperCase(Locale.ROOT);

        final String destinationCode =
                to.trim().toUpperCase(Locale.ROOT);

        if (sourceCode.equals(destinationCode)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Source and destination must be different"
            );
        }

        List<Train> trains = trainRepository.searchTrains(
                sourceCode, destinationCode
        );

        return trains.stream()
                .map(train -> toResponse(
                        train, sourceCode, destinationCode
                ))
                .toList();
    }

    private TrainSearchResponse toResponse(
            Train train, String from, String to) {

        TrainStop origin = train.getStops().stream()
                .filter(stop -> stop.getStation()
                        .getCode().equals(from))
                .findFirst()
                .orElseThrow();

        TrainStop destination = train.getStops().stream()
                .filter(stop -> stop.getStation()
                        .getCode().equals(to))
                .findFirst()
                .orElseThrow();

        BigDecimal fare = destination.getFareFromOrigin()
                .subtract(origin.getFareFromOrigin());

        return new TrainSearchResponse(
                train.getTrainNumber(),
                train.getName(),
                origin.getStation().getCode(),
                origin.getStation().getName(),
                origin.getDepartureTime(),
                destination.getStation().getCode(),
                destination.getStation().getName(),
                destination.getArrivalTime(),
                fare
        );
    }
}