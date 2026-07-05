package com.chalchitraghar.modules.movies.validation;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

/**
 * Validates movie poster URLs accepted by the API.
 */
@Documented
@Constraint(validatedBy = PosterUrlValidator.class)
@Target(FIELD)
@Retention(RUNTIME)
public @interface ValidPosterUrl {
    String message() default "Poster URL must be a valid http or https URL";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
