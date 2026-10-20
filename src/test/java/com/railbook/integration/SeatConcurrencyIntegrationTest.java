
package com.railbook.integration;

import com.railbook.dto.SeatHoldResponse;
import com.railbook.entity.Seat;
import com.railbook.entity.SeatInventory;
import com.railbook.entity.SeatStatus;
import com.railbook.entity.Train;
import com.railbook.entity.Trip;
import com.railbook.repository.SeatInventoryRepository;
import com.railbook.repository.SeatRepository;
import com.railbook.repository.TrainRepository;
import com.railbook.repository.TripRepository;
import com.railbook.service.SeatHoldService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("integration")
class SeatConcurrencyIntegrationTest {

    @Autowired
    private SeatHoldService seatHoldService;

    @Autowired
    private TrainRepository trainRepository;

    @Autowired
    private TripRepository tripRepository;

    @Autowired
    private SeatRepository seatRepository;

    @Autowired
    private SeatInventoryRepository inventoryRepository;

    @Test
    void onlyOneRequestShouldHoldTheSameSeat() throws Exception {
        Train train = trainRepository.saveAndFlush(
                new Train(
                        "C" + java.util.UUID.randomUUID()
                                .toString().replace("-", "").substring(0, 9),
                        "Concurrency Test Train"
                )
        );

        Trip trip = tripRepository.saveAndFlush(
                new Trip(train, LocalDate.now().plusDays(5))
        );

        Seat seat = seatRepository.saveAndFlush(
                new Seat(train, "C1", "01", "SL")
        );

        SeatInventory inventory = inventoryRepository.saveAndFlush(
                new SeatInventory(trip, seat)
        );

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        Callable<Boolean> attemptHold = () -> {
            ready.countDown();

            if (!start.await(5, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Start signal timed out");
            }

            try {
                SeatHoldResponse response =
                        seatHoldService.holdSeat(trip.getId(), seat.getId());

                return SeatStatus.HELD.name().equals(response.status());
            } catch (ResponseStatusException ex) {
                if (ex.getStatusCode().value() == 409) {
                    return false;
                }
                throw ex;
            }
        };

        try {
            Future<Boolean> first = executor.submit(attemptHold);
            Future<Boolean> second = executor.submit(attemptHold);

            assertTrue(ready.await(5, TimeUnit.SECONDS));
            start.countDown();

            boolean firstSucceeded = first.get(15, TimeUnit.SECONDS);
            boolean secondSucceeded = second.get(15, TimeUnit.SECONDS);

            assertNotEquals(
                    firstSucceeded,
                    secondSucceeded,
                    "Exactly one request should hold the seat"
            );

            SeatInventory persisted =
                    inventoryRepository.findById(inventory.getId()).orElseThrow();

            assertEquals(SeatStatus.HELD, persisted.getStatus());
            assertNotNull(persisted.getHoldToken());
            assertNotNull(persisted.getHoldExpiresAt());
        } finally {
            start.countDown();
            executor.shutdownNow();
        }
    }
}
