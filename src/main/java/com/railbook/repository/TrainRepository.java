package com.railbook.repository;

import com.railbook.entity.Train;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TrainRepository extends JpaRepository<Train, Long> {

    Optional<Train> findByTrainNumber(String trainNumber);

    @Query("""
        SELECT DISTINCT t
        FROM Train t
        JOIN t.stops origin
        JOIN t.stops destination
        WHERE origin.station.code = :from
          AND destination.station.code = :to
          AND origin.stopOrder < destination.stopOrder
          AND t.active = true
        ORDER BY t.trainNumber
        """)
    List<Train> searchTrains(
            @Param("from") String from,
            @Param("to") String to
    );
}