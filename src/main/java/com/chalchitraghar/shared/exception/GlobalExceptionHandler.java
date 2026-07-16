package com.chalchitraghar.shared.exception;

import com.chalchitraghar.shared.observability.LogSanitizer;
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
import org.springframework.web.servlet.resource.NoResourceFoundException;

/** Global exception handler providing centralized error handling for the application. */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleResourceNotFoundException(
            ResourceNotFoundException ex) {
        logger.atInfo()
                .addKeyValue("event", "request.failed")
                .addKeyValue("failureCode", "resource_not_found")
                .log("Resource not found: {}", LogSanitizer.safe(ex.getMessage()));
        return error(HttpStatus.NOT_FOUND, messageOrDefault(ex, "Resource not found"));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<Void>> handleAuthenticationException(
            AuthenticationException ex) {
        logger.atWarn()
                .addKeyValue("event", "security.authentication_failed")
                .addKeyValue("failureCode", "authentication_failed")
                .log("Authentication failed");
        return error(HttpStatus.UNAUTHORIZED, messageOrDefault(ex, "Authentication failed"));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDeniedException(AccessDeniedException ex) {
        logger.atWarn()
                .addKeyValue("event", "security.access_denied")
                .addKeyValue("failureCode", "access_denied")
                .log("Access denied");
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
        logger.atWarn()
                .addKeyValue("event", "request.failed")
                .addKeyValue("failureCode", "conflict")
                .log("Request conflict: {}", LogSanitizer.safe(ex.getMessage()));
        return error(HttpStatus.CONFLICT, messageOrDefault(ex, "Conflict error"));
    }

    @ExceptionHandler({InvalidSeatSelectionException.class, IllegalArgumentException.class})
    public ResponseEntity<ApiResponse<Void>> handleBadRequestExceptions(RuntimeException ex) {
        logger.atInfo()
                .addKeyValue("event", "request.failed")
                .addKeyValue("failureCode", "bad_request")
                .log("Bad request: {}", LogSanitizer.safe(ex.getMessage()));
        return error(HttpStatus.BAD_REQUEST, messageOrDefault(ex, "Bad request"));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationExceptions(
            MethodArgumentNotValidException ex) {
        logger.atInfo()
                .addKeyValue("event", "request.failed")
                .addKeyValue("failureCode", "validation_failed")
                .log("Request validation failed");
        List<String> errors =
                ex.getBindingResult().getFieldErrors().stream()
                        .map(error -> error.getField() + ": " + error.getDefaultMessage())
                        .toList();
        return ResponseEntity.badRequest().body(ApiResponse.error("Validation failed", errors));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraintViolationException(
            ConstraintViolationException ex) {
        logger.atInfo()
                .addKeyValue("event", "request.failed")
                .addKeyValue("failureCode", "constraint_violation")
                .log("Request constraint validation failed");
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
        logger.atInfo()
                .addKeyValue("event", "request.failed")
                .addKeyValue("failureCode", "malformed_body")
                .log("Malformed request body");
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

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNoResourceFoundException(
            NoResourceFoundException ex) {
        return error(HttpStatus.NOT_FOUND, "The requested resource does not exist");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataIntegrityViolationException(
            DataIntegrityViolationException ex) {
        logger.atWarn()
                .addKeyValue("event", "request.failed")
                .addKeyValue("failureCode", "data_integrity_conflict")
                .log("Database integrity conflict");
        return error(HttpStatus.CONFLICT, getDetails(ex));
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataAccessException(DataAccessException ex) {
        logger.atError()
                .setCause(ex)
                .addKeyValue("event", "request.failed")
                .addKeyValue("failureCode", "database_unavailable")
                .addKeyValue("exceptionType", ex.getClass().getSimpleName())
                .log("Unexpected database access failure");
        return error(
                HttpStatus.INTERNAL_SERVER_ERROR, "An error occurred while accessing the database");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGenericException(Exception ex) {
        logger.atError()
                .setCause(ex)
                .addKeyValue("event", "request.failed")
                .addKeyValue("failureCode", "internal_error")
                .addKeyValue("exceptionType", ex.getClass().getSimpleName())
                .log("Unexpected request processing failure");
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
