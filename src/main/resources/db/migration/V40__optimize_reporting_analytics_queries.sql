CREATE INDEX idx_bookings_user_status_time
    ON bookings (user_id, status, booking_time);

CREATE INDEX idx_refunds_requested_status_currency
    ON refunds (requested_at, status, currency);
