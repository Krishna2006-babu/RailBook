package com.railbook.config;

import com.railbook.entity.Station;
import com.railbook.entity.Train;
import com.railbook.entity.TrainStop;
import com.railbook.repository.StationRepository;
import com.railbook.repository.TrainRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.time.LocalTime;

@Configuration
public class DataSeeder {

    @Bean
    CommandLineRunner seedData(
            StationRepository stationRepository,
            TrainRepository trainRepository) {

        return args -> {
            Station siliguri = getOrCreateStation(
                    stationRepository,
                    "SGUJ",
                    "Siliguri Junction",
                    "Siliguri"
            );

            Station newJalpaiguri = getOrCreateStation(
                    stationRepository,
                    "NJP",
                    "New Jalpaiguri",
                    "Jalpaiguri"
            );

            Station guwahati = getOrCreateStation(
                    stationRepository,
                    "GHY",
                    "Guwahati",
                    "Guwahati"
            );

            if (trainRepository
                    .findByTrainNumber("90001")
                    .isEmpty()) {

                Train train = new Train(
                        "90001",
                        "RailBook Express"
                );

                train.addStop(new TrainStop(
                        siliguri,
                        1,
                        null,
                        LocalTime.of(20, 0),
                        new BigDecimal("0.00")
                ));

                train.addStop(new TrainStop(
                        newJalpaiguri,
                        2,
                        LocalTime.of(20, 30),
                        LocalTime.of(20, 35),
                        new BigDecimal("120.00")
                ));

                train.addStop(new TrainStop(
                        guwahati,
                        3,
                        LocalTime.of(6, 30),
                        null,
                        new BigDecimal("450.00")
                ));

                trainRepository.save(train);
            }
        };
    }

    private Station getOrCreateStation(
            StationRepository repository,
            String code,
            String name,
            String city) {

        return repository.findByCode(code)
                .orElseGet(() -> repository.save(
                        new Station(code, name, city)
                ));
    }
}