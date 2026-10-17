
package com.railbook.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(
        name = "bookings",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_booking_pnr",
                        columnNames = "pnr"
                ),
                @UniqueConstraint(
                        name = "uk_booking_trip_seat",
                        columnNames = {"trip_id", "seat_id"}
                )
        }
)
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 12)
    private String pnr;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "trip_id", nullable = false)
    private Trip trip;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "seat_id", nullable = false)
    private Seat seat;

    @Column(name = "booked_at", nullable = false)
    private Instant bookedAt;

    protected Booking() {
    }

    public Booking(String pnr, Trip trip, Seat seat, Instant bookedAt) {
        this.pnr = pnr;
        this.trip = trip;
        this.seat = seat;
        this.bookedAt = bookedAt;
    }

    public Long getId() {
        return id;
    }

    public String getPnr() {
        return pnr;
    }

    public Trip getTrip() {
        return trip;
    }

    public Seat getSeat() {
        return seat;
    }

    public Instant getBookedAt() {
        return bookedAt;
    }
}
