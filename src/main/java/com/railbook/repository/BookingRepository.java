
package com.railbook.repository;

import com.railbook.entity.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    Optional<Booking> findByPnr(String pnr);

    @EntityGraph(attributePaths = {"trip", "trip.train", "seat"})
    List<Booking> findByTripIdOrderByBookedAtDesc(Long tripId);

    @EntityGraph(attributePaths = {"trip", "trip.train", "seat"})
    List<Booking> findByUserIdOrderByBookedAtDesc(Long userId);

    boolean existsByPnr(String pnr);
    Optional<Booking> findByPnrAndUser_Email(String pnr, String email);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
    SELECT b
    FROM Booking b
    WHERE b.pnr = :pnr
      AND LOWER(b.user.email) = LOWER(:email)
    """)
    Optional<Booking> findByPnrAndUserEmailForUpdate(
            @Param("pnr") String pnr,
            @Param("email") String email
    );
}
