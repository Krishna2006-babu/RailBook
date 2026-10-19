
package com.railbook.service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import com.railbook.entity.SeatInventory;
import com.railbook.entity.SeatStatus;
import com.railbook.repository.SeatInventoryRepository;
import com.railbook.repository.BookingRepository;
import com.railbook.repository.AppUserRepository;
import com.railbook.service.PnrGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.railbook.entity.AppUser;
import java.util.Optional;
import com.railbook.dto.SeatConfirmationResponse;
import com.railbook.entity.Trip;
import com.railbook.entity.Seat;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import com.railbook.entity.Booking;
import java.util.List;
import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

class SeatHoldServiceTest {

    private SeatInventoryRepository inventoryRepository;
    private BookingRepository bookingRepository;
    private PnrGenerator pnrGenerator;
    private AppUserRepository appUserRepository;
    private SeatHoldService seatHoldService;

    @BeforeEach
    void setUp() {
        inventoryRepository = mock(SeatInventoryRepository.class);
        bookingRepository = mock(BookingRepository.class);
        pnrGenerator = mock(PnrGenerator.class);
        appUserRepository = mock(AppUserRepository.class);

        seatHoldService = new SeatHoldService(
                inventoryRepository,
                bookingRepository,
                pnrGenerator,
                appUserRepository
        );
    }


    @Test
    void shouldRejectAlreadyHeldSeat() {
        Long tripId = 1L;
        Long seatId = 2L;

        SeatInventory inventory = mock(SeatInventory.class);

        when(inventoryRepository.findForUpdate(tripId, seatId))
                .thenReturn(Optional.of(inventory));

        when(inventory.getStatus()).thenReturn(SeatStatus.HELD);
        when(inventory.getHoldExpiresAt())
                .thenReturn(java.time.Instant.now().plusSeconds(300));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> seatHoldService.holdSeat(tripId, seatId)
        );

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());

        verify(inventory, never()).hold(anyString(), any());
    }
    @Test
    void shouldRejectAlreadyBookedSeat() {
        Long tripId = 1L;
        Long seatId = 2L;

        SeatInventory inventory = mock(SeatInventory.class);

        when(inventoryRepository.findForUpdate(tripId, seatId))
                .thenReturn(Optional.of(inventory));

        when(inventory.getStatus()).thenReturn(SeatStatus.BOOKED);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> seatHoldService.holdSeat(tripId, seatId)
        );

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());

        verify(inventory, never()).hold(anyString(), any());
    }
    @Test
    void shouldReturnNotFoundWhenSeatInventoryDoesNotExist() {
        Long tripId = 1L;
        Long seatId = 999L;

        when(inventoryRepository.findForUpdate(tripId, seatId))
                .thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> seatHoldService.holdSeat(tripId, seatId)
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());

        verify(inventoryRepository).findForUpdate(tripId, seatId);
    }
    @Test
    void shouldAllowHoldingSeatAfterPreviousHoldExpires() {
        Long tripId = 1L;
        Long seatId = 2L;

        SeatInventory inventory = mock(SeatInventory.class);

        when(inventoryRepository.findForUpdate(tripId, seatId))
                .thenReturn(Optional.of(inventory));

        when(inventory.getStatus()).thenReturn(SeatStatus.HELD);
        when(inventory.getHoldExpiresAt())
                .thenReturn(java.time.Instant.now().minusSeconds(60));

        var response = seatHoldService.holdSeat(tripId, seatId);

        assertNotNull(response);
        verify(inventory).clearHold();
        verify(inventory).hold(anyString(), any());
    }
    @Test
    void shouldHoldAvailableSeat() {
        Long tripId = 1L;
        Long seatId = 2L;

        SeatInventory inventory = mock(SeatInventory.class);

        when(inventoryRepository.findForUpdate(tripId, seatId))
                .thenReturn(Optional.of(inventory));
        when(inventory.getStatus()).thenReturn(SeatStatus.AVAILABLE);

        var response = seatHoldService.holdSeat(tripId, seatId);

        assertNotNull(response);
        assertEquals(tripId, response.tripId());
        assertEquals(seatId, response.seatId());
        assertEquals(SeatStatus.HELD.name(), response.status());
        assertNotNull(response.holdToken());
        assertFalse(response.holdToken().isBlank());
        assertNotNull(response.expiresAt());
        assertTrue(response.expiresAt().isAfter(java.time.Instant.now()));
        verify(inventory).hold(anyString(), any());
    }
    @Test
    void shouldRejectConfirmationWhenUserIsNotAuthenticated() {
        SecurityContextHolder.clearContext();

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> seatHoldService.confirmSeat(1L, 2L, "test-token")
        );

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());

        verifyNoInteractions(appUserRepository, inventoryRepository,
                bookingRepository, pnrGenerator);
    }
    @Test
    void shouldRejectConfirmationWithInvalidHoldToken() {
        Long tripId = 1L;
        Long seatId = 2L;

        var authentication = new org.springframework.security.authentication
                .UsernamePasswordAuthenticationToken(
                "krishna.test@example.com", null,
                java.util.List.of(
                        new org.springframework.security.core.authority
                                .SimpleGrantedAuthority("ROLE_USER")
                )
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        try {
            AppUser user = mock(AppUser.class);
            SeatInventory inventory = mock(SeatInventory.class);

            when(appUserRepository.findByEmail("krishna.test@example.com"))
                    .thenReturn(Optional.of(user));

            when(inventoryRepository.findForUpdate(tripId, seatId))
                    .thenReturn(Optional.of(inventory));

            when(inventory.getStatus()).thenReturn(SeatStatus.HELD);
            when(inventory.getHoldToken()).thenReturn("correct-token");

            ResponseStatusException exception = assertThrows(
                    ResponseStatusException.class,
                    () -> seatHoldService.confirmSeat(
                            tripId, seatId, "wrong-token"
                    )
            );

            assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());

            verifyNoInteractions(bookingRepository, pnrGenerator);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }
    @Test
    void shouldRejectConfirmationWhenHoldHasExpired() {
        Long tripId = 1L;
        Long seatId = 2L;

        var authentication =
                new org.springframework.security.authentication
                        .UsernamePasswordAuthenticationToken(
                        "krishna.test@example.com",
                        null,
                        java.util.List.of(
                                new org.springframework.security.core.authority
                                        .SimpleGrantedAuthority("ROLE_USER")
                        )
                );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        try {
            AppUser user = mock(AppUser.class);
            SeatInventory inventory = mock(SeatInventory.class);

            when(appUserRepository.findByEmail("krishna.test@example.com"))
                    .thenReturn(Optional.of(user));
            when(inventoryRepository.findForUpdate(tripId, seatId))
                    .thenReturn(Optional.of(inventory));
            when(inventory.getStatus()).thenReturn(SeatStatus.HELD);
            when(inventory.getHoldToken()).thenReturn("valid-token");
            when(inventory.getHoldExpiresAt())
                    .thenReturn(java.time.Instant.now().minusSeconds(60));

            ResponseStatusException exception = assertThrows(
                    ResponseStatusException.class,
                    () -> seatHoldService.confirmSeat(
                            tripId, seatId, "valid-token"
                    )
            );

            assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());

            verifyNoInteractions(bookingRepository, pnrGenerator);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Test
    void shouldConfirmBookingSuccessfully() {
        Long tripId = 1L;
        Long seatId = 2L;
        String email = "krishna.test@example.com";
        String token = "valid-token";
        String pnr = "1234567890";

        var authentication = new UsernamePasswordAuthenticationToken(
                email,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        try {
            AppUser user = mock(AppUser.class);
            SeatInventory inventory = mock(SeatInventory.class);
            Trip trip = mock(Trip.class);
            Seat seat = mock(Seat.class);

            when(appUserRepository.findByEmail(email))
                    .thenReturn(Optional.of(user));
            when(inventoryRepository.findForUpdate(tripId, seatId))
                    .thenReturn(Optional.of(inventory));
            when(inventory.getStatus()).thenReturn(SeatStatus.HELD);
            when(inventory.getHoldToken()).thenReturn(token);
            when(inventory.getHoldExpiresAt())
                    .thenReturn(Instant.now().plusSeconds(300));
            when(inventory.getTrip()).thenReturn(trip);
            when(inventory.getSeat()).thenReturn(seat);
            when(pnrGenerator.generate()).thenReturn(pnr);
            when(bookingRepository.existsByPnr(pnr)).thenReturn(false);

            SeatConfirmationResponse response =
                    seatHoldService.confirmSeat(tripId, seatId, token);

            assertNotNull(response);
            assertEquals(tripId, response.tripId());
            assertEquals(seatId, response.seatId());
            assertEquals(SeatStatus.BOOKED.name(), response.status());
            assertEquals(pnr, response.pnr());
            assertNotNull(response.confirmedAt());

            verify(bookingRepository).saveAndFlush(any(Booking.class));
            verify(inventory).confirmBooking();
            verify(pnrGenerator).generate();
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Test
    void shouldRetryWhenGeneratedPnrAlreadyExists() {
        Long tripId = 1L;
        Long seatId = 2L;
        String email = "krishna.test@example.com";
        String token = "valid-token";

        var authentication = new UsernamePasswordAuthenticationToken(
                email,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        try {
            AppUser user = mock(AppUser.class);
            SeatInventory inventory = mock(SeatInventory.class);

            when(appUserRepository.findByEmail(email))
                    .thenReturn(Optional.of(user));
            when(inventoryRepository.findForUpdate(tripId, seatId))
                    .thenReturn(Optional.of(inventory));
            when(inventory.getStatus()).thenReturn(SeatStatus.HELD);
            when(inventory.getHoldToken()).thenReturn(token);
            when(inventory.getHoldExpiresAt())
                    .thenReturn(Instant.now().plusSeconds(300));

            when(pnrGenerator.generate())
                    .thenReturn("1234567890", "9876543210");

            when(bookingRepository.existsByPnr("1234567890"))
                    .thenReturn(true);
            when(bookingRepository.existsByPnr("9876543210"))
                    .thenReturn(false);

            SeatConfirmationResponse response =
                    seatHoldService.confirmSeat(tripId, seatId, token);

            assertEquals("9876543210", response.pnr());

            verify(pnrGenerator, times(2)).generate();
            verify(bookingRepository).saveAndFlush(any(Booking.class));
            verify(inventory).confirmBooking();
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Test
    void shouldFailWhenUniquePnrCannotBeGenerated() {
        Long tripId = 1L;
        Long seatId = 2L;
        String email = "krishna.test@example.com";
        String token = "valid-token";

        var authentication = new UsernamePasswordAuthenticationToken(
                email,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        try {
            AppUser user = mock(AppUser.class);
            SeatInventory inventory = mock(SeatInventory.class);

            when(appUserRepository.findByEmail(email))
                    .thenReturn(Optional.of(user));
            when(inventoryRepository.findForUpdate(tripId, seatId))
                    .thenReturn(Optional.of(inventory));
            when(inventory.getStatus()).thenReturn(SeatStatus.HELD);
            when(inventory.getHoldToken()).thenReturn(token);
            when(inventory.getHoldExpiresAt())
                    .thenReturn(Instant.now().plusSeconds(300));

            when(pnrGenerator.generate()).thenReturn("1234567890");
            when(bookingRepository.existsByPnr("1234567890"))
                    .thenReturn(true);

            ResponseStatusException exception = assertThrows(
                    ResponseStatusException.class,
                    () -> seatHoldService.confirmSeat(tripId, seatId, token)
            );

            assertEquals(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    exception.getStatusCode()
            );

            verify(pnrGenerator, times(5)).generate();
            verify(bookingRepository, never()).saveAndFlush(any(Booking.class));
            verify(inventory, never()).confirmBooking();
        } finally {
            SecurityContextHolder.clearContext();
        }
    }


}
