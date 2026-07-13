package com.chalchitraghar.shared.exception;

/** Exception thrown when a movie business rule conflict occurs. */
public class MovieConflictException extends RuntimeException {
    public MovieConflictException(String message) {
        super(message);
    }
}
