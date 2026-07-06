DROP INDEX IF EXISTS uk_halls_name_normalized;

CREATE UNIQUE INDEX uk_halls_name_normalized ON halls (LOWER(TRIM(name)));
