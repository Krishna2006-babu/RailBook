package com.railbook.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "seat_inventory",
        uniqueConstraints = @UniqueConstraint(
                columnNames = {"trip_id", "seat_id"}
        ),
        indexes = @Index(
                name = "idx_inventory_trip_status",
                columnList = "trip_id,status"
        ))
public class SeatInventory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "trip_id", nullable = false)
    private Trip trip;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "seat_id", nullable = false)
    private Seat seat;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SeatStatus status = SeatStatus.AVAILABLE;

    @Column(name = "hold_token")
    private String holdToken;

    @Column(name = "hold_expires_at")
    private Instant holdExpiresAt;

    protected SeatInventory() {
    }

    public SeatInventory(Trip trip, Seat seat) {
        this.trip = trip;
        this.seat = seat;
        this.status = SeatStatus.AVAILABLE;
    }

    public void hold(String token, Instant expiresAt) {
        this.status = SeatStatus.HELD;
        this.holdToken = token;
        this.holdExpiresAt = expiresAt;
    }

    public void clearHold() {
        this.status = SeatStatus.AVAILABLE;
        this.holdToken = null;
        this.holdExpiresAt = null;
    }

    public void confirmBooking() {
        this.status = SeatStatus.BOOKED;
        this.holdToken = null;
        this.holdExpiresAt = null;
    }

    public Long getId() {
        return id;
    }

    public Trip getTrip() {
        return trip;
    }

    public Seat getSeat() {
        return seat;
    }

    public SeatStatus getStatus() {
        return status;
    }

    public String getHoldToken() {
        return holdToken;
    }

    public Instant getHoldExpiresAt() {
        return holdExpiresAt;
    }
}