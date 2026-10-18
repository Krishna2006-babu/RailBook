
package com.railbook.repository;

import com.railbook.entity.SeatInventory;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SeatInventoryRepository
        extends JpaRepository<SeatInventory, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT i
        FROM SeatInventory i
        WHERE i.trip.id = :tripId
          AND i.seat.id = :seatId
        """)
    Optional<SeatInventory> findForUpdate(
            @Param("tripId") Long tripId,
            @Param("seatId") Long seatId);

    List<SeatInventory> findByTripId(Long tripId);

    @Query("""
    SELECT i.id
    FROM SeatInventory i
    WHERE i.status = com.railbook.entity.SeatStatus.HELD
      AND i.holdExpiresAt IS NOT NULL
      AND i.holdExpiresAt <= :now
    ORDER BY i.holdExpiresAt
    """)
    List<Long> findExpiredHoldIds(@Param("now") java.time.Instant now);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
    SELECT i
    FROM SeatInventory i
    WHERE i.id = :inventoryId
    """)
    Optional<SeatInventory> findByIdForUpdate(
            @Param("inventoryId") Long inventoryId);
    boolean existsByTripIdAndSeatId(Long tripId, Long seatId);
}
