package com.railbook.config;

import com.railbook.entity.*;
import com.railbook.repository.*;
import jakarta.transaction.Transactional;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDate;
import java.util.List;

@Configuration
public class SeatInventorySeeder {

    @Bean
    CommandLineRunner seedInventory(
            TrainRepository trainRepository,
            TripRepository tripRepository,
            SeatRepository seatRepository,
            SeatInventoryRepository inventoryRepository) {

        return args -> {
            Train train = trainRepository
                    .findByTrainNumber("90001")
                    .orElseThrow();

            LocalDate date = LocalDate.now().plusDays(1);

            Trip trip = tripRepository
                    .findByTrainIdAndJourneyDate(
                            train.getId(), date)
                    .orElseGet(() -> tripRepository.save(
                            new Trip(train, date)));

            List<String> seatNumbers = List.of("01", "02", "03");

            for (String number : seatNumbers) {
                Seat seat = seatRepository
                        .findByTrainIdAndCoachNumberAndSeatNumber(
                                train.getId(), "S1", number)
                        .orElseGet(() -> seatRepository.save(
                                new Seat(train, "S1", number, "SL")));

                if (!inventoryRepository.existsByTripIdAndSeatId(
                        trip.getId(), seat.getId())) {
                    inventoryRepository.save(
                            new SeatInventory(trip, seat));
                }
            }
        };
    }
}