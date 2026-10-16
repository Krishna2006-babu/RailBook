package com.railbook.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalTime;

@Entity
@Table(name = "train_stops", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"train_id", "stop_order"}),
        @UniqueConstraint(columnNames = {"train_id", "station_id"})
})
public class TrainStop {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "train_id", nullable = false)
    private Train train;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "station_id", nullable = false)
    private Station station;

    @Column(name = "stop_order", nullable = false)
    private int stopOrder;

    @Column(name = "arrival_time")
    private LocalTime arrivalTime;

    @Column(name = "departure_time")
    private LocalTime departureTime;

    @Column(name = "fare_from_origin", nullable = false,
            precision = 10, scale = 2)
    private BigDecimal fareFromOrigin;

    protected TrainStop() {
    }

    public TrainStop(Station station, int stopOrder,
                     LocalTime arrivalTime, LocalTime departureTime,
                     BigDecimal fareFromOrigin) {
        this.station = station;
        this.stopOrder = stopOrder;
        this.arrivalTime = arrivalTime;
        this.departureTime = departureTime;
        this.fareFromOrigin = fareFromOrigin;
    }

    void setTrain(Train train) {
        this.train = train;
    }

    public Station getStation() {
        return station;
    }

    public int getStopOrder() {
        return stopOrder;
    }

    public LocalTime getArrivalTime() {
        return arrivalTime;
    }

    public LocalTime getDepartureTime() {
        return departureTime;
    }

    public BigDecimal getFareFromOrigin() {
        return fareFromOrigin;
    }
}