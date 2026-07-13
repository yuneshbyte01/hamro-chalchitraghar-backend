package com.chalchitraghar.shared.exception;

import com.chalchitraghar.shared.response.ApiResponse;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ConstraintViolationException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/** Global exception handler providing centralized error handling for the application. */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleResourceNotFoundException(
            ResourceNotFoundException ex) {
        logger.warn("Resource not found: {}", ex.getMessage());
        return error(HttpStatus.NOT_FOUND, messageOrDefault(ex, "Resource not found"));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<Void>> handleAuthenticationException(
            AuthenticationException ex) {
        logger.warn("Authentication failed: {}", ex.getMessage());
        return error(HttpStatus.UNAUTHORIZED, messageOrDefault(ex, "Authentication failed"));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDeniedException(AccessDeniedException ex) {
        logger.warn("Access denied: {}", ex.getMessage());
        return error(HttpStatus.FORBIDDEN, messageOrDefault(ex, "Access denied"));
    }

    @ExceptionHandler({
        HallConflictException.class,
        MovieConflictException.class,
        ShowConflictException.class,
        SeatAlreadyBookedException.class,
        SeatLockedException.class,
        PaymentConflictException.class,
        InvalidBookingStateException.class
    })
    public ResponseEntity<ApiResponse<Void>> handleConflictExceptions(RuntimeException ex) {
        logger.warn("Conflict: {}", ex.getMessage());
        return error(HttpStatus.CONFLICT, messageOrDefault(ex, "Conflict error"));
    }

    @ExceptionHandler({InvalidSeatSelectionException.class, IllegalArgumentException.class})
    public ResponseEntity<ApiResponse<Void>> handleBadRequestExceptions(RuntimeException ex) {
        logger.warn("Bad request: {}", ex.getMessage());
        return error(HttpStatus.BAD_REQUEST, messageOrDefault(ex, "Bad request"));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationExceptions(
            MethodArgumentNotValidException ex) {
        logger.warn("Validation error: {}", ex.getMessage());
        List<String> errors =
                ex.getBindingResult().getFieldErrors().stream()
                        .map(error -> error.getField() + ": " + error.getDefaultMessage())
                        .toList();
        return ResponseEntity.badRequest().body(ApiResponse.error("Validation failed", errors));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraintViolationException(
            ConstraintViolationException ex) {
        logger.warn("Constraint violation: {}", ex.getMessage());
        List<String> errors =
                ex.getConstraintViolations().stream()
                        .map(
                                violation ->
                                        violation.getPropertyPath() + ": " + violation.getMessage())
                        .toList();
        return ResponseEntity.badRequest().body(ApiResponse.error("Validation failed", errors));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodArgumentTypeMismatchException(
            MethodArgumentTypeMismatchException ex) {
        logger.warn("Type mismatch for parameter '{}': {}", ex.getName(), ex.getMessage());
        return error(
                HttpStatus.BAD_REQUEST,
                String.format("Parameter '%s' has invalid type", ex.getName()));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingServletRequestParameterException(
            MissingServletRequestParameterException ex) {
        logger.warn("Missing required parameter: {}", ex.getParameterName());
        return error(
                HttpStatus.BAD_REQUEST,
                String.format("Required parameter '%s' is missing", ex.getParameterName()));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleHttpMessageNotReadableException(
            HttpMessageNotReadableException ex) {
        logger.warn("Malformed request body: {}", ex.getMessage());
        return error(HttpStatus.BAD_REQUEST, "The request body is invalid or cannot be parsed");
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodNotSupported(
            HttpRequestMethodNotSupportedException ex) {
        logger.warn("Method not supported: {}", ex.getMethod());
        return error(HttpStatus.METHOD_NOT_ALLOWED, "Request method is not supported");
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleEntityNotFoundException(
            EntityNotFoundException ex) {
        logger.warn("Entity not found: {}", ex.getMessage());
        return error(HttpStatus.NOT_FOUND, messageOrDefault(ex, "Entity not found"));
    }

    @ExceptionHandler(EmptyResultDataAccessException.class)
    public ResponseEntity<ApiResponse<Void>> handleEmptyResultDataAccessException(
            EmptyResultDataAccessException ex) {
        logger.warn("No result found: {}", ex.getMessage());
        return error(HttpStatus.NOT_FOUND, "The requested resource does not exist");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataIntegrityViolationException(
            DataIntegrityViolationException ex) {
        logger.error("Data integrity violation: ", ex);
        return error(HttpStatus.CONFLICT, getDetails(ex));
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataAccessException(DataAccessException ex) {
        logger.error("Data access error: ", ex);
        return error(
                HttpStatus.INTERNAL_SERVER_ERROR, "An error occurred while accessing the database");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGenericException(Exception ex) {
        logger.error("Unexpected error: ", ex);
        return error(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred while processing your request");
    }

    private ResponseEntity<ApiResponse<Void>> error(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(ApiResponse.error(message));
    }

    private String messageOrDefault(Exception ex, String defaultMessage) {
        return ex.getMessage() == null || ex.getMessage().isBlank()
                ? defaultMessage
                : ex.getMessage();
    }

    private static String getDetails(DataIntegrityViolationException ex) {
        String details = "A data integrity constraint was violated";
        if (ex.getCause() != null && ex.getCause().getMessage() != null) {
            String causeMessage = ex.getCause().getMessage();
            if (causeMessage.contains("unique constraint")
                    || causeMessage.contains("Unique index")) {
                details = "A resource with this value already exists";
            } else if (causeMessage.contains("foreign key constraint")) {
                details = "Cannot perform this operation due to existing references";
            }
        }
        return details;
    }
}
