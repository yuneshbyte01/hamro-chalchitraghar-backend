CREATE TABLE shows (
    id BIGSERIAL PRIMARY KEY,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    movie_id BIGINT NOT NULL,
    hall_id BIGINT NOT NULL,
    status VARCHAR(255) NOT NULL,
    show_date DATE NOT NULL,
    show_time TIME NOT NULL,
    end_time TIME NOT NULL,
    CONSTRAINT fk_shows_movie
        FOREIGN KEY (movie_id) REFERENCES movies (id),
    CONSTRAINT fk_shows_hall
        FOREIGN KEY (hall_id) REFERENCES halls (id)
);
