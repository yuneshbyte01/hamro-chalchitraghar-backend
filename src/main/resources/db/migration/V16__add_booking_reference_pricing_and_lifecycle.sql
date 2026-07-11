ALTER TABLE bookings ADD COLUMN booking_reference VARCHAR(50);
ALTER TABLE bookings ADD COLUMN total_amount NUMERIC(12,2);
ALTER TABLE bookings ADD COLUMN currency VARCHAR(3) NOT NULL DEFAULT 'NPR';
ALTER TABLE bookings ADD COLUMN expires_at TIMESTAMP;
ALTER TABLE bookings ADD COLUMN confirmed_at TIMESTAMP;
ALTER TABLE bookings ADD COLUMN cancelled_at TIMESTAMP;
ALTER TABLE bookings ADD COLUMN expired_at TIMESTAMP;
ALTER TABLE booking_seats ADD COLUMN unit_price NUMERIC(12,2);

UPDATE bookings
SET booking_reference = 'HCG-' || TO_CHAR(booking_time, 'YYYYMMDD') || '-' || UPPER(SUBSTRING(MD5(id::text || booking_time::text) FROM 1 FOR 8));
UPDATE booking_seats bs SET unit_price = s.price FROM seats s WHERE s.id = bs.seat_id;
UPDATE bookings b SET total_amount = COALESCE((
    SELECT SUM(bs.unit_price) FROM booking_seats bs WHERE bs.booking_id = b.id
), 0.00);

ALTER TABLE bookings ALTER COLUMN booking_reference SET NOT NULL;
ALTER TABLE bookings ALTER COLUMN total_amount SET NOT NULL;
ALTER TABLE booking_seats ALTER COLUMN unit_price SET NOT NULL;
ALTER TABLE bookings ADD CONSTRAINT uk_bookings_reference UNIQUE (booking_reference);
ALTER TABLE booking_seats ADD CONSTRAINT uk_booking_seats_booking_seat UNIQUE (booking_id, seat_id);

CREATE INDEX idx_bookings_reference ON bookings (booking_reference);
CREATE INDEX idx_bookings_user_time ON bookings (user_id, booking_time);
CREATE INDEX idx_bookings_show_status ON bookings (show_id, status);
CREATE INDEX idx_bookings_status_expiry ON bookings (status, expires_at);
CREATE INDEX idx_booking_seats_booking ON booking_seats (booking_id);
CREATE INDEX idx_booking_seats_seat ON booking_seats (seat_id);
