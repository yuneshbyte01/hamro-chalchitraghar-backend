ALTER TABLE movies ADD COLUMN title_normalized VARCHAR(255);

ALTER TABLE movies ALTER COLUMN poster_url TYPE VARCHAR(500);

UPDATE movies
SET title_normalized = lower(trim(title));

ALTER TABLE movies ALTER COLUMN title_normalized SET NOT NULL;

CREATE UNIQUE INDEX uk_movies_title_normalized_release_date
    ON movies (title_normalized, release_date);
