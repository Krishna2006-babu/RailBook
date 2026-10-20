
package com.railbook.integration;
import com.railbook.dto.SeatConfirmationResponse;
import com.railbook.entity.AppUser;
import com.railbook.entity.Booking;
import com.railbook.repository.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import java.util.List;
import org.springframework.web.server.ResponseStatusException;
import jakarta.persistence.EntityManager;
import java.util.concurrent.atomic.AtomicInteger;
import com.railbook.dto.SeatHoldResponse;
import com.railbook.entity.Seat;
import com.railbook.entity.SeatInventory;
import com.railbook.entity.SeatStatus;
import com.railbook.entity.Train;
import com.railbook.entity.Trip;
import com.railbook.service.SeatHoldService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.server.ResponseStatusException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
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
    @Autowired
    private BookingRepository bookingRepository;
    @Autowired
    private EntityManager entityManager;
    @Autowired
    private AppUserRepository appUserRepository;
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

    @Test
    void onlyOneOfFiftyConcurrentRequestsShouldHoldTheSameSeat() throws Exception {
        Train train = trainRepository.saveAndFlush(
                new Train(
                        "C" + UUID.randomUUID()
                                .toString().replace("-", "").substring(0, 9),
                        "Fifty Request Test Train"
                )
        );

        Trip trip = tripRepository.saveAndFlush(
                new Trip(train, LocalDate.now().plusDays(6))
        );

        Seat seat = seatRepository.saveAndFlush(
                new Seat(train, "C1", "01", "SL")
        );

        SeatInventory inventory = inventoryRepository.saveAndFlush(
                new SeatInventory(trip, seat)
        );

        int requestCount = 50;
        ExecutorService executor = Executors.newFixedThreadPool(requestCount);
        CountDownLatch ready = new CountDownLatch(requestCount);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Boolean>> futures = new ArrayList<>();

        Callable<Boolean> attemptHold = () -> {
            ready.countDown();

            if (!start.await(10, TimeUnit.SECONDS)) {
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
            for (int i = 0; i < requestCount; i++) {
                futures.add(executor.submit(attemptHold));
            }

            assertTrue(ready.await(15, TimeUnit.SECONDS),
                    "All requests should be ready before the start signal");

            start.countDown();

            int successCount = 0;

            for (Future<Boolean> future : futures) {
                if (future.get(30, TimeUnit.SECONDS)) {
                    successCount++;
                }
            }

            assertEquals(1, successCount,
                    "Exactly one of 50 requests should hold the seat");

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

    @Test
    void onlyOneOfTwoConcurrentRequestsShouldConfirmTheSameSeat() throws Exception {
        Train train = trainRepository.saveAndFlush(
                new Train(
                        "C" + UUID.randomUUID()
                                .toString().replace("-", "").substring(0, 9),
                        "Concurrent Confirmation Test"
                )
        );

        Trip trip = tripRepository.saveAndFlush(
                new Trip(train, LocalDate.now().plusDays(7))
        );

        Seat seat = seatRepository.saveAndFlush(
                new Seat(train, "C1", "01", "SL")
        );

        SeatInventory inventory = inventoryRepository.saveAndFlush(
                new SeatInventory(trip, seat)
        );

        String email = "confirm-" + UUID.randomUUID() + "@railbook.test";

        appUserRepository.saveAndFlush(
                new AppUser("Concurrency Tester", email, "test-hash")
        );

        var authentication = new UsernamePasswordAuthenticationToken(
                email,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        var holdResponse = seatHoldService.holdSeat(
                trip.getId(), seat.getId()
        );

        assertEquals("HELD", holdResponse.status());
        String holdToken = holdResponse.holdToken();

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        Callable<Boolean> confirmAttempt = () -> {
            SecurityContextHolder.getContext()
                    .setAuthentication(authentication);

            ready.countDown();

            try {
                if (!start.await(10, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("Start signal timed out");
                }

                SeatConfirmationResponse response =
                        seatHoldService.confirmSeat(
                                trip.getId(), seat.getId(), holdToken
                        );

                return "BOOKED".equals(response.status());
            } catch (ResponseStatusException ex) {
                if (ex.getStatusCode().value() == 400
                        || ex.getStatusCode().value() == 409) {
                    return false;
                }
                throw ex;
            } finally {
                SecurityContextHolder.clearContext();
            }
        };

        try {
            Future<Boolean> first = executor.submit(confirmAttempt);
            Future<Boolean> second = executor.submit(confirmAttempt);

            assertTrue(ready.await(10, TimeUnit.SECONDS));
            start.countDown();

            boolean firstResult = first.get(20, TimeUnit.SECONDS);
            boolean secondResult = second.get(20, TimeUnit.SECONDS);

            assertNotEquals(firstResult, secondResult,
                    "Exactly one confirmation should succeed");

            entityManager.clear();

            SeatInventory persisted = inventoryRepository
                    .findById(inventory.getId())
                    .orElseThrow();

            assertEquals(SeatStatus.BOOKED, persisted.getStatus());
            assertNull(persisted.getHoldToken());
            assertNull(persisted.getHoldExpiresAt());

            long bookingCount = bookingRepository
                    .findByTripIdOrderByBookedAtDesc(trip.getId())
                    .stream()
                    .filter(b -> b.getSeat().getId().equals(seat.getId()))
                    .count();

            assertEquals(1, bookingCount,
                    "Exactly one booking should exist for this seat");
        } finally {
            start.countDown();
            executor.shutdownNow();
            SecurityContextHolder.clearContext();
        }
    }

}
