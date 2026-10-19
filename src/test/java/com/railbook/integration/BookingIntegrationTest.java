
package com.railbook.integration;

import com.railbook.dto.SeatConfirmationResponse;
import com.railbook.entity.*;
import com.railbook.repository.*;
import com.railbook.service.SeatHoldService;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("integration")
class BookingIntegrationTest {

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
    private AppUserRepository appUserRepository;

    @Autowired
    private EntityManager entityManager;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldPersistBookingAndUpdateSeatStatus() {

        Train train = trainRepository.saveAndFlush(
                new Train("IT90001", "Integration Test Express")
        );

        Trip trip = tripRepository.saveAndFlush(
                new Trip(train, LocalDate.now().plusDays(10))
        );

        Seat seat = seatRepository.saveAndFlush(
                new Seat(train, "S1", "1", "SLEEPER")
        );

        SeatInventory inventory = inventoryRepository.saveAndFlush(
                new SeatInventory(trip, seat)
        );

        String email = "integration-test@railbook.test";

        appUserRepository.saveAndFlush(
                new AppUser("Integration Tester", email, "test-hash")
        );

        var authentication =
                new UsernamePasswordAuthenticationToken(
                        email,
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_USER"))
                );

        SecurityContextHolder.getContext()
                .setAuthentication(authentication);

        var holdResponse = seatHoldService.holdSeat(
                trip.getId(), seat.getId()
        );

        assertEquals("HELD", holdResponse.status());

        SeatConfirmationResponse confirmation =
                seatHoldService.confirmSeat(
                        trip.getId(),
                        seat.getId(),
                        holdResponse.holdToken()
                );

        assertEquals("BOOKED", confirmation.status());
        assertNotNull(confirmation.pnr());

        Booking savedBooking = bookingRepository
                .findByPnr(confirmation.pnr())
                .orElseThrow();

        assertEquals(confirmation.pnr(), savedBooking.getPnr());
        assertEquals(trip.getId(), savedBooking.getTrip().getId());
        assertEquals(seat.getId(), savedBooking.getSeat().getId());
        assertEquals(
                email,
                appUserRepository.findByEmail(email)
                        .orElseThrow()
                        .getEmail()
        );

        SeatInventory savedInventory = inventoryRepository
                .findById(inventory.getId())
                .orElseThrow();

        assertEquals(SeatStatus.BOOKED, savedInventory.getStatus());
        assertNull(savedInventory.getHoldToken());
        assertNull(savedInventory.getHoldExpiresAt());
    }
}
