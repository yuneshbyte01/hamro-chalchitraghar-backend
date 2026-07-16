CREATE INDEX idx_bookings_booking_time_status
    ON bookings (booking_time, status);

CREATE INDEX idx_payments_status_completed_currency
    ON payments (status, completed_at, currency);

CREATE INDEX idx_refunds_status_processed_currency
    ON refunds (status, processed_at, currency);

CREATE INDEX idx_shows_date_status
    ON shows (show_date, status);

CREATE INDEX idx_shows_movie_date_status
    ON shows (movie_id, show_date, status);

CREATE INDEX idx_shows_hall_date_status
    ON shows (hall_id, show_date, status);

CREATE INDEX idx_seats_show_status
    ON seats (show_id, seat_status);
