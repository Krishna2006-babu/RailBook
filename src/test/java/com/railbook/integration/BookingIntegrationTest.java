
package com.railbook.integration;

import com.railbook.dto.SeatConfirmationResponse;
import com.railbook.entity.*;
import com.railbook.repository.*;
import com.railbook.service.SeatHoldService;
import com.railbook.dto.BookingResponse;
import com.railbook.service.BookingService;
import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
@SpringBootTest
@AutoConfigureMockMvc
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

    @Autowired
    private RollbackTestService rollbackTestService;
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private BookingService bookingService;
    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }
    private String uniqueTrainNumber() {
        return "IT" + UUID.randomUUID()
                .toString()
                .replace("-", "")
                .substring(0, 8)
                .toUpperCase();
    }
    @Test
    void shouldPersistBookingAndUpdateSeatStatus() {

        Train train = trainRepository.saveAndFlush(
                new Train(uniqueTrainNumber(), "Integration Test Express")
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

        String email = "integration-test-" + UUID.randomUUID()
                + "@railbook.test";

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

    @Test
    void shouldRollbackBookingAndSeatStatusOnFailure() {

        Train train = trainRepository.saveAndFlush(
                new Train(uniqueTrainNumber(), "Rollback Test Express")
        );

        Trip trip = tripRepository.saveAndFlush(
                new Trip(train, LocalDate.now().plusDays(11))
        );

        Seat seat = seatRepository.saveAndFlush(
                new Seat(train, "S2", "1", "SLEEPER")
        );

        SeatInventory inventory = inventoryRepository.saveAndFlush(
                new SeatInventory(trip, seat)
        );

        inventory.hold(
                "rollback-test-token",
                Instant.now().plusSeconds(300)
        );

        inventoryRepository.saveAndFlush(inventory);

        String pnr = "9876543210";

        Booking booking = new Booking(
                pnr,
                trip,
                seat,
                Instant.now()
        );

        assertThrows(
                IllegalStateException.class,
                () -> rollbackTestService.saveBookingAndFail(
                        booking,
                        inventory.getId()
                )
        );

        entityManager.clear();

        assertFalse(bookingRepository.findByPnr(pnr).isPresent());

        SeatInventory persistedInventory = inventoryRepository
                .findById(inventory.getId())
                .orElseThrow();

        assertEquals(SeatStatus.HELD, persistedInventory.getStatus());
        assertEquals(
                "rollback-test-token",
                persistedInventory.getHoldToken()
        );
        assertNotNull(persistedInventory.getHoldExpiresAt());
    }


    @Test
    void databaseShouldRejectDuplicateBookingForSameTripAndSeat() {
        Train train = trainRepository.saveAndFlush(
                new Train("DB" + java.util.UUID.randomUUID()
                        .toString().replace("-", "").substring(0, 8),
                        "Database Constraint Test")
        );

        Trip trip = tripRepository.saveAndFlush(
                new Trip(train, LocalDate.now().plusDays(12))
        );

        Seat seat = seatRepository.saveAndFlush(
                new Seat(train, "DC1", "01", "SLEEPER")
        );

        Booking firstBooking = new Booking(
                "PNR" + java.util.UUID.randomUUID()
                        .toString().replace("-", "").substring(0, 7),
                trip,
                seat,
                Instant.now()
        );

        bookingRepository.saveAndFlush(firstBooking);

        Booking duplicateBooking = new Booking(
                "PNR" + java.util.UUID.randomUUID()
                        .toString().replace("-", "").substring(0, 7),
                trip,
                seat,
                Instant.now()
        );

        assertThrows(
                org.springframework.dao.DataIntegrityViolationException.class,
                () -> bookingRepository.saveAndFlush(duplicateBooking),
                "Database must reject a second active booking for the same trip and seat"
        );
    }

    @Test
    void onlyBookingOwnerShouldAccessBookingByPnr() throws Exception {
        String ownerEmail = "pnr-owner-" + java.util.UUID.randomUUID()
                + "@railbook.test";
        String otherEmail = "pnr-other-" + java.util.UUID.randomUUID()
                + "@railbook.test";

        AppUser owner = appUserRepository.saveAndFlush(
                new AppUser("Booking Owner", ownerEmail, "test-hash")
        );

        appUserRepository.saveAndFlush(
                new AppUser("Other User", otherEmail, "test-hash")
        );

        Train train = trainRepository.saveAndFlush(
                new Train(
                        "AU" + java.util.UUID.randomUUID()
                                .toString().replace("-", "").substring(0, 8),
                        "Authorization Test Express"
                )
        );

        Trip trip = tripRepository.saveAndFlush(
                new Trip(train, LocalDate.now().plusDays(20))
        );

        Seat seat = seatRepository.saveAndFlush(
                new Seat(train, "AU1", "01", "SLEEPER")
        );

        String pnr = "PNR" + java.util.UUID.randomUUID()
                .toString().replace("-", "").substring(0, 7);

        bookingRepository.saveAndFlush(
                new Booking(pnr, trip, seat, owner, Instant.now())
        );

        mockMvc.perform(get("/api/bookings/{pnr}", pnr)
                        .with(user(ownerEmail).roles("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pnr").value(pnr));

        mockMvc.perform(get("/api/bookings/{pnr}", pnr)
                        .with(user(otherEmail).roles("USER")))
                .andExpect(status().isNotFound());
    }
    @Test
    void myBookingsShouldOnlyReturnCurrentUsersBookings() throws Exception {
        String email1 = "my-bookings-1-" + java.util.UUID.randomUUID()
                + "@railbook.test";
        String email2 = "my-bookings-2-" + java.util.UUID.randomUUID()
                + "@railbook.test";

        AppUser user1 = appUserRepository.saveAndFlush(
                new AppUser("User One", email1, "test-hash")
        );

        AppUser user2 = appUserRepository.saveAndFlush(
                new AppUser("User Two", email2, "test-hash")
        );

        Train train = trainRepository.saveAndFlush(
                new Train(
                        "MB" + java.util.UUID.randomUUID()
                                .toString().replace("-", "").substring(0, 8),
                        "My Bookings Test Express"
                )
        );

        Trip trip = tripRepository.saveAndFlush(
                new Trip(train, LocalDate.now().plusDays(25))
        );

        Seat seat1 = seatRepository.saveAndFlush(
                new Seat(train, "MB1", "01", "SLEEPER")
        );

        Seat seat2 = seatRepository.saveAndFlush(
                new Seat(train, "MB1", "02", "SLEEPER")
        );

        String pnr1 = "PNR" + java.util.UUID.randomUUID()
                .toString().replace("-", "").substring(0, 7);
        String pnr2 = "PNR" + java.util.UUID.randomUUID()
                .toString().replace("-", "").substring(0, 7);

        bookingRepository.saveAndFlush(
                new Booking(pnr1, trip, seat1, user1, Instant.now())
        );

        bookingRepository.saveAndFlush(
                new Booking(pnr2, trip, seat2, user2, Instant.now())
        );

        mockMvc.perform(get("/api/bookings/my")
                        .with(user(email1).roles("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].pnr").value(pnr1));
    }

    @Test
    void postRequestWithoutCsrfTokenShouldBeForbidden() throws Exception {
        mockMvc.perform(
                org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}")
        ).andExpect(status().isForbidden());
    }

    @Test
    void postRequestWithCsrfTokenShouldPassCsrfCheck() throws Exception {
        mockMvc.perform(
                org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}")
        ).andExpect(status().isBadRequest());
    }

    @Test
    void loginShouldCreateAuthenticatedSessionWithCsrfProtection() throws Exception {
        String email = "csrf-login-" + java.util.UUID.randomUUID()
                + "@railbook.test";
        String password = "StrongTestPassword123!";

        String registerJson = """
            {
              "name": "CSRF Test User",
              "email": "%s",
              "password": "%s"
            }
            """.formatted(email, password);

        mockMvc.perform(
                org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson)
        ).andExpect(status().isCreated());

        String loginJson = """
            {
              "email": "%s",
              "password": "%s"
            }
            """.formatted(email, password);

        MvcResult loginResult = mockMvc.perform(
                        org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                                .post("/api/auth/login")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(loginJson)
                )
                .andExpect(status().isOk())
                .andReturn();

        assertNotNull(loginResult.getRequest().getSession(false));

        mockMvc.perform(
                org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .get("/api/bookings/my")
                        .session(
                                (org.springframework.mock.web.MockHttpSession)
                                        loginResult.getRequest().getSession(false)
                        )
        ).andExpect(status().isOk());
    }
    @Test
    void logoutWithoutCsrfTokenShouldBeForbidden() throws Exception {
        mockMvc.perform(post("/api/auth/logout"))
                .andExpect(status().isForbidden());
    }
    @Test
    void logoutWithCsrfTokenShouldInvalidateSession() throws Exception {
        String email = "logout-" + UUID.randomUUID() + "@railbook.test";
        String password = "StrongTestPassword123!";

        String registerJson = """
        {"name":"Logout Test User","email":"%s","password":"%s"}
        """.formatted(email, password);

        mockMvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson))
                .andExpect(status().isCreated());

        String loginJson = """
        {"email":"%s","password":"%s"}
        """.formatted(email, password);

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson))
                .andExpect(status().isOk())
                .andReturn();

        MockHttpSession session =
                (MockHttpSession) loginResult.getRequest().getSession(false);

        assertNotNull(session);

        mockMvc.perform(post("/api/auth/logout")
                        .session(session)
                        .with(csrf()))
                .andExpect(status().isNoContent());

        assertTrue(session.isInvalid());

        mockMvc.perform(get("/api/bookings/my")
                        .session(session))
                .andExpect(status().isUnauthorized());
    }
    @Test
    void nonExistentPnrShouldReturnNotFound() throws Exception {
        String email = "pnr-missing-" + UUID.randomUUID()
                + "@railbook.test";

        mockMvc.perform(get("/api/bookings/{pnr}", "PNR-NOT-FOUND")
                        .with(user(email).roles("USER")))
                .andExpect(status().isNotFound());
    }
    @Test
    void bookingWithoutOwnerShouldNotBeAccessible() throws Exception {
        Train train = trainRepository.saveAndFlush(
                new Train(
                        "NU" + UUID.randomUUID()
                                .toString().replace("-", "").substring(0, 8),
                        "No Owner Test Express"
                )
        );

        Trip trip = tripRepository.saveAndFlush(
                new Trip(train, LocalDate.now().plusDays(30))
        );

        Seat seat = seatRepository.saveAndFlush(
                new Seat(train, "NU1", "01", "SLEEPER")
        );

        String pnr = "PNR" + UUID.randomUUID()
                .toString().replace("-", "").substring(0, 7);

        bookingRepository.saveAndFlush(
                new Booking(pnr, trip, seat, Instant.now())
        );

        mockMvc.perform(get("/api/bookings/{pnr}", pnr)
                        .with(user("any-user@railbook.test").roles("USER")))
                .andExpect(status().isNotFound());
    }
    @Test
    void cancellingBookingShouldReleaseSeatForRebooking() {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        String email = "cancel-" + suffix + "@railbook.test";

        AppUser user = appUserRepository.saveAndFlush(
                new AppUser("Cancellation Tester", email, "test-hash")
        );

        Train train = trainRepository.saveAndFlush(
                new Train("CA" + suffix, "Cancellation Test Express")
        );

        Trip trip = tripRepository.saveAndFlush(
                new Trip(train, LocalDate.now().plusDays(15))
        );

        Seat seat = seatRepository.saveAndFlush(
                new Seat(train, "CA1", "01", "SLEEPER")
        );

        SeatInventory inventory = new SeatInventory(trip, seat);
        inventory.confirmBooking();
        inventory = inventoryRepository.saveAndFlush(inventory);

        Booking originalBooking = bookingRepository.saveAndFlush(
                new Booking(
                        "PNR" + suffix,
                        trip,
                        seat,
                        user,
                        Instant.now()
                )
        );

        BookingResponse cancelled = bookingService.cancelBooking(
                originalBooking.getPnr(),
                email
        );

        assertEquals("CANCELLED", cancelled.status());

        entityManager.clear();

        Booking persistedBooking = bookingRepository
                .findByPnr(originalBooking.getPnr())
                .orElseThrow();

        SeatInventory persistedInventory = inventoryRepository
                .findById(inventory.getId())
                .orElseThrow();

        assertEquals(BookingStatus.CANCELLED, persistedBooking.getStatus());
        assertEquals(SeatStatus.AVAILABLE, persistedInventory.getStatus());

        String newPnr = "PNR" + UUID.randomUUID()
                .toString().replace("-", "").substring(0, 7);

        Booking newBooking = bookingRepository.saveAndFlush(
                new Booking(newPnr, trip, seat, user, Instant.now())
        );

        assertEquals(BookingStatus.CONFIRMED, newBooking.getStatus());

        assertEquals(
                BookingStatus.CANCELLED,
                bookingRepository.findByPnr(originalBooking.getPnr())
                        .orElseThrow()
                        .getStatus()
        );
    }

    @Test
    void cancellingAnotherUsersBookingShouldReturnNotFound() {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8);

        AppUser owner = appUserRepository.saveAndFlush(
                new AppUser("Booking Owner", "owner-" + suffix + "@railbook.test", "test-hash")
        );

        Train train = trainRepository.saveAndFlush(
                new Train("OW" + suffix, "Ownership Test Express")
        );

        Trip trip = tripRepository.saveAndFlush(
                new Trip(train, LocalDate.now().plusDays(16))
        );

        Seat seat = seatRepository.saveAndFlush(
                new Seat(train, "OW1", "01", "SLEEPER")
        );

        SeatInventory inventory = new SeatInventory(trip, seat);
        inventory.confirmBooking();
        inventoryRepository.saveAndFlush(inventory);

        Booking booking = bookingRepository.saveAndFlush(
                new Booking(
                        "PNR" + suffix,
                        trip,
                        seat,
                        owner,
                        Instant.now()
                )
        );

        assertThrows(
                org.springframework.web.server.ResponseStatusException.class,
                () -> bookingService.cancelBooking(
                        booking.getPnr(),
                        "someone-else-" + suffix + "@railbook.test"
                )
        );

        entityManager.clear();

        assertEquals(
                BookingStatus.CONFIRMED,
                bookingRepository.findByPnr(booking.getPnr())
                        .orElseThrow()
                        .getStatus()
        );

        assertEquals(
                SeatStatus.BOOKED,
                inventoryRepository.findById(inventory.getId())
                        .orElseThrow()
                        .getStatus()
        );
    }
    @Test
    void cancellingAlreadyCancelledBookingShouldReturnConflict() {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        String email = "repeat-cancel-" + suffix + "@railbook.test";

        AppUser user = appUserRepository.saveAndFlush(
                new AppUser("Repeat Cancellation Tester", email, "test-hash")
        );

        Train train = trainRepository.saveAndFlush(
                new Train("RC" + suffix, "Repeat Cancellation Express")
        );

        Trip trip = tripRepository.saveAndFlush(
                new Trip(train, LocalDate.now().plusDays(17))
        );

        Seat seat = seatRepository.saveAndFlush(
                new Seat(train, "RC1", "01", "SLEEPER")
        );

        SeatInventory inventory = new SeatInventory(trip, seat);
        inventory.confirmBooking();
        inventoryRepository.saveAndFlush(inventory);

        Booking booking = bookingRepository.saveAndFlush(
                new Booking(
                        "PNR" + suffix,
                        trip,
                        seat,
                        user,
                        Instant.now()
                )
        );

        bookingService.cancelBooking(booking.getPnr(), email);

        org.springframework.web.server.ResponseStatusException exception =
                assertThrows(
                        org.springframework.web.server.ResponseStatusException.class,
                        () -> bookingService.cancelBooking(booking.getPnr(), email)
                );

        assertEquals(
                org.springframework.http.HttpStatus.CONFLICT,
                exception.getStatusCode()
        );
    }
}