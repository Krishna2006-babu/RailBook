package com.railbook.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "seats",
        uniqueConstraints = @UniqueConstraint(
                columnNames = {"train_id", "coach_number", "seat_number"}
        ))
public class Seat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "train_id", nullable = false)
    private Train train;

    @Column(name = "coach_number", nullable = false)
    private String coachNumber;

    @Column(name = "seat_number", nullable = false)
    private String seatNumber;

    @Column(nullable = false)
    private String seatClass;

    protected Seat() {
    }

    public Seat(Train train, String coachNumber,
                String seatNumber, String seatClass) {
        this.train = train;
        this.coachNumber = coachNumber;
        this.seatNumber = seatNumber;
        this.seatClass = seatClass;
    }

    public Long getId() {
        return id;
    }

    public Train getTrain() {
        return train;
    }

    public String getCoachNumber() {
        return coachNumber;
    }

    public String getSeatNumber() {
        return seatNumber;
    }

    public String getSeatClass() {
        return seatClass;
    }
}