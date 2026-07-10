ALTER TABLE seat_templates
    ADD CONSTRAINT uk_seat_templates_hall_code UNIQUE (hall_id, seat_code),
    ADD CONSTRAINT uk_seat_templates_hall_row_number UNIQUE (hall_id, row_label, seat_number),
    ADD CONSTRAINT uk_seat_templates_hall_position UNIQUE (hall_id, position_index);

ALTER TABLE seats
    ADD CONSTRAINT uk_seats_show_code UNIQUE (show_id, seat_code),
    ADD CONSTRAINT uk_seats_show_row_number UNIQUE (show_id, row_label, seat_number),
    ADD CONSTRAINT uk_seats_show_position UNIQUE (show_id, position_index);
