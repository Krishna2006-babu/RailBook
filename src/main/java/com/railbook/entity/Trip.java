package com.railbook.entity;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "trips",
        uniqueConstraints = @UniqueConstraint(
                columnNames = {"train_id", "journey_date"}
        ))
public class Trip {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "train_id", nullable = false)
    private Train train;

    @Column(name = "journey_date", nullable = false)
    private LocalDate journeyDate;

    protected Trip() {
    }

    public Trip(Train train, LocalDate journeyDate) {
        this.train = train;
        this.journeyDate = journeyDate;
    }

    public Long getId() {
        return id;
    }

    public Train getTrain() {
        return train;
    }

    public LocalDate getJourneyDate() {
        return journeyDate;
    }
}