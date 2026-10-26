package com.railbook.entity;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "trains", uniqueConstraints = {
        @UniqueConstraint(
                name = "uk_train_number",
                columnNames = "train_number"
        )
})
public class Train {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "train_number", nullable = false, length = 10)
    private String trainNumber;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private boolean active = true;

    @OneToMany(mappedBy = "train", cascade = CascadeType.ALL,
            orphanRemoval = true)
    @OrderBy("stopOrder ASC")
    private List<TrainStop> stops = new ArrayList<>();

    protected Train() {
    }

    public Train(String trainNumber, String name) {
        this.trainNumber = trainNumber;
        this.name = name;
        this.active = true;
    }

    public void addStop(TrainStop stop) {
        stops.add(stop);
        stop.setTrain(this);
    }

    public Long getId() {
        return id;
    }

    public String getTrainNumber() {
        return trainNumber;
    }

    public String getName() {
        return name;
    }

    public boolean isActive() {
        return active;
    }

    public List<TrainStop> getStops() {
        return stops;
    }
}