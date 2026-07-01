CREATE TABLE movies (
    id BIGSERIAL PRIMARY KEY,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    title VARCHAR(255) NOT NULL,
    genre VARCHAR(255) NOT NULL,
    duration_minutes INTEGER NOT NULL,
    language VARCHAR(255) NOT NULL,
    description VARCHAR(255) NOT NULL,
    poster_url VARCHAR(255) NOT NULL,
    release_date DATE NOT NULL,
    status VARCHAR(255) NOT NULL
);
