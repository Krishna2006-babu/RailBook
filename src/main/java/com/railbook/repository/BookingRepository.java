
package com.railbook.repository;

import com.railbook.entity.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;

import java.util.List;
import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    Optional<Booking> findByPnr(String pnr);

    @EntityGraph(attributePaths = {"trip", "trip.train", "seat"})
    List<Booking> findByTripIdOrderByBookedAtDesc(Long tripId);

    @EntityGraph(attributePaths = {"trip", "trip.train", "seat"})
    List<Booking> findByUserIdOrderByBookedAtDesc(Long userId);

    boolean existsByPnr(String pnr);
}
