package com.chalchitraghar.shared.exception;

/** Raised when a show lifecycle operation conflicts with dependent data or status. */
public class ShowConflictException extends RuntimeException {
    public ShowConflictException(String message) {
        super(message);
    }
}
