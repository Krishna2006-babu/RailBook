package com.railbook.repository;

import com.railbook.entity.Seat;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SeatRepository extends JpaRepository<Seat, Long> {

    List<Seat> findByTrainId(Long trainId);

    Optional<Seat> findByTrainIdAndCoachNumberAndSeatNumber(
            Long trainId, String coachNumber, String seatNumber);
}