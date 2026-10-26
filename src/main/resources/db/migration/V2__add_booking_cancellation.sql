
ALTER TABLE bookings
    ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'CONFIRMED';

ALTER TABLE bookings
    ADD CONSTRAINT chk_booking_status
        CHECK (status IN ('CONFIRMED', 'CANCELLED'));

ALTER TABLE bookings
    DROP CONSTRAINT uk_booking_trip_seat;

CREATE UNIQUE INDEX uk_booking_active_trip_seat
    ON bookings (trip_id, seat_id)
    WHERE status = 'CONFIRMED';
