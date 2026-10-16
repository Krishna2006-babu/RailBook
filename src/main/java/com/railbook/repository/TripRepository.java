package com.railbook.repository;

import com.railbook.entity.Trip;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface TripRepository extends JpaRepository<Trip, Long> {

    Optional<Trip> findByTrainIdAndJourneyDate(
            Long trainId, LocalDate journeyDate);
}