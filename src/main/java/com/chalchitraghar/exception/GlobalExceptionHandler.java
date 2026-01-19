package com.chalchitraghar.exception;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import com.chalchitraghar.dto.error.ErrorResponse;

import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;

/**
 * Global exception handler providing centralized error handling for the application.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Handles ResourceNotFoundException and returns a 404 Not Found response.
     *
     * @param ex the exception to handle
     * @param request the web request
     * @return the response entity with the error response
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFoundException(
            ResourceNotFoundException ex, WebRequest request) {
        logger.warn("Resource not found: {}", ex.getMessage());
        ErrorResponse errorResponse = ErrorResponse.builder()
                .status(HttpStatus.NOT_FOUND.value())
                .message("Resource not found")
                .details(ex.getMessage())
                .timestamp(LocalDateTime.now())
                .errorCode("RESOURCE_NOT_FOUND")
                .path(getRequestPath(request))
                .build();
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

    /**
     * Handles AuthenticationException and returns a 401 Unauthorized response.
     *
     * @param ex the exception to handle
     * @param request the web request
     * @return the response entity with the error response
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthenticationException(
            AuthenticationException ex, WebRequest request) {
        logger.warn("Authentication failed: {}", ex.getMessage());
        ErrorResponse errorResponse = ErrorResponse.builder()
                .status(HttpStatus.UNAUTHORIZED.value())
                .message("Authentication failed")
                .details(ex.getMessage())
                .timestamp(LocalDateTime.now())
                .errorCode("AUTHENTICATION_FAILED")
                .path(getRequestPath(request))
                .build();
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
    }

    /**
     * Handles AccessDeniedException and returns a 403 Forbidden response.
     *
     * @param ex the exception to handle
     * @param request the web request
     * @return the response entity with the error response
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDeniedException(
            AccessDeniedException ex, WebRequest request) {
        logger.warn("Access denied: {}", ex.getMessage());
        ErrorResponse errorResponse = ErrorResponse.builder()
                .status(HttpStatus.FORBIDDEN.value())
                .message("Access denied")
                .details("You do not have permission to access this resource")
                .timestamp(LocalDateTime.now())
                .errorCode("ACCESS_DENIED")
                .path(getRequestPath(request))
                .build();
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
    }

    /**
     * Handles HallConflictException and returns a 409 Conflict response.
     *
     * @param ex the exception to handle
     * @param request the web request
     * @return the response entity with the error response
     */
    @ExceptionHandler(HallConflictException.class)
    public ResponseEntity<ErrorResponse> handleHallConflictException(
            HallConflictException ex, WebRequest request) {
        logger.warn("Hall conflict: {}", ex.getMessage());
        ErrorResponse errorResponse = ErrorResponse.builder()
                .status(HttpStatus.CONFLICT.value())
                .message("Hall conflict")
                .details(ex.getMessage())
                .timestamp(LocalDateTime.now())
                .errorCode("HALL_CONFLICT")
                .path(getRequestPath(request))
                .build();
        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
    }

    /**
     * Handles SeatAlreadyBookedException and returns a 409 Conflict response.
     *
     * @param ex the exception to handle
     * @param request the web request
     * @return the response entity with the error response
     */
    @ExceptionHandler(SeatAlreadyBookedException.class)
    public ResponseEntity<ErrorResponse> handleSeatAlreadyBookedException(
            SeatAlreadyBookedException ex, WebRequest request) {
        logger.warn("Seat already booked: {}", ex.getMessage());
        ErrorResponse errorResponse = ErrorResponse.builder()
                .status(HttpStatus.CONFLICT.value())
                .message("Seat already booked")
                .details(ex.getMessage())
                .timestamp(LocalDateTime.now())
                .errorCode("SEAT_ALREADY_BOOKED")
                .path(getRequestPath(request))
                .build();
        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
    }

    /**
     * Handles SeatLockedException and returns a 409 Conflict response.
     *
     * @param ex the exception to handle
     * @param request the web request
     * @return the response entity with the error response
     */
    @ExceptionHandler(SeatLockedException.class)
    public ResponseEntity<ErrorResponse> handleSeatLockedException(
            SeatLockedException ex, WebRequest request) {
        logger.warn("Seat locked: {}", ex.getMessage());
        ErrorResponse errorResponse = ErrorResponse.builder()
                .status(HttpStatus.CONFLICT.value())
                .message("Seat locked")
                .details(ex.getMessage())
                .timestamp(LocalDateTime.now())
                .errorCode("SEAT_LOCKED")
                .path(getRequestPath(request))
                .build();
        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
    }

    /**
     * Handles InvalidSeatSelectionException and returns a 400 Bad Request response.
     *
     * @param ex the exception to handle
     * @param request the web request
     * @return the response entity with the error response
     */
    @ExceptionHandler(InvalidSeatSelectionException.class)
    public ResponseEntity<ErrorResponse> handleInvalidSeatSelectionException(
            InvalidSeatSelectionException ex, WebRequest request) {
        logger.warn("Invalid seat selection: {}", ex.getMessage());
        ErrorResponse errorResponse = ErrorResponse.builder()
                .status(HttpStatus.BAD_REQUEST.value())
                .message("Invalid seat selection")
                .details(ex.getMessage())
                .timestamp(LocalDateTime.now())
                .errorCode("INVALID_SEAT_SELECTION")
                .path(getRequestPath(request))
                .build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    /**
     * Handles IllegalArgumentException and returns a 400 Bad Request response.
     *
     * @param ex the exception to handle
     * @param request the web request
     * @return the response entity with the error response
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(
            IllegalArgumentException ex, WebRequest request) {
        logger.warn("Invalid argument: {}", ex.getMessage());
        ErrorResponse errorResponse = ErrorResponse.builder()
                .status(HttpStatus.BAD_REQUEST.value())
                .message("Invalid argument")
                .details(ex.getMessage())
                .timestamp(LocalDateTime.now())
                .errorCode("INVALID_ARGUMENT")
                .path(getRequestPath(request))
                .build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    /**
     * Handles MethodArgumentNotValidException and returns a 400 Bad Request response.
     *
     * @param ex the exception to handle
     * @param request the web request
     * @return the response entity with the error response
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationExceptions(
            MethodArgumentNotValidException ex, WebRequest request) {
        logger.warn("Validation error: {}", ex.getMessage());
        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error -> fieldErrors.put(error.getField(), error.getDefaultMessage()));
        ErrorResponse errorResponse = ErrorResponse.builder()
                .status(HttpStatus.BAD_REQUEST.value())
                .message("Validation failed")
                .details("One or more fields have validation errors")
                .timestamp(LocalDateTime.now())
                .errorCode("VALIDATION_FAILED")
                .fieldErrors(fieldErrors)
                .path(getRequestPath(request))
                .build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    /**
     * Handles ConstraintViolationException and returns a 400 Bad Request response.
     *
     * @param ex the exception to handle
     * @param request the web request
     * @return the response entity with the error response
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolationException(
            ConstraintViolationException ex, WebRequest request) {
        logger.warn("Constraint violation: {}", ex.getMessage());
        Map<String, String> fieldErrors = ex.getConstraintViolations().stream()
                .collect(Collectors.toMap(
                        violation -> violation.getPropertyPath().toString(),
                        ConstraintViolation::getMessage));
        ErrorResponse errorResponse = ErrorResponse.builder()
                .status(HttpStatus.BAD_REQUEST.value())
                .message("Constraint violation")
                .details("One or more constraints were violated")
                .timestamp(LocalDateTime.now())
                .errorCode("CONSTRAINT_VIOLATION")
                .fieldErrors(fieldErrors)
                .path(getRequestPath(request))
                .build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    /**
     * Handles MethodArgumentTypeMismatchException and returns a 400 Bad Request response.
     *
     * @param ex the exception to handle
     * @param request the web request
     * @return the response entity with the error response
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentTypeMismatchException(
            MethodArgumentTypeMismatchException ex, WebRequest request) {
        logger.warn("Type mismatch for parameter '{}': {}", ex.getName(), ex.getMessage());
        ErrorResponse errorResponse = ErrorResponse.builder()
                .status(HttpStatus.BAD_REQUEST.value())
                .message("Invalid parameter type")
                .details(String.format("Parameter '%s' has invalid type", ex.getName()))
                .timestamp(LocalDateTime.now())
                .errorCode("TYPE_MISMATCH")
                .path(getRequestPath(request))
                .build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    /**
     * Handles MissingServletRequestParameterException and returns a 400 Bad Request response.
     *
     * @param ex the exception to handle
     * @param request the web request
     * @return the response entity with the error response
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingServletRequestParameterException(
            MissingServletRequestParameterException ex, WebRequest request) {
        logger.warn("Missing required parameter: {}", ex.getParameterName());
        ErrorResponse errorResponse = ErrorResponse.builder()
                .status(HttpStatus.BAD_REQUEST.value())
                .message("Missing required parameter")
                .details(String.format("Required parameter '%s' is missing", ex.getParameterName()))
                .timestamp(LocalDateTime.now())
                .errorCode("MISSING_PARAMETER")
                .path(getRequestPath(request))
                .build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    /**
     * Handles HttpMessageNotReadableException and returns a 400 Bad Request response.
     *
     * @param ex the exception to handle
     * @param request the web request
     * @return the response entity with the error response
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadableException(
            HttpMessageNotReadableException ex, WebRequest request) {
        logger.warn("Malformed request body: {}", ex.getMessage());
        ErrorResponse errorResponse = ErrorResponse.builder()
                .status(HttpStatus.BAD_REQUEST.value())
                .message("Malformed request body")
                .details("The request body is invalid or cannot be parsed")
                .timestamp(LocalDateTime.now())
                .errorCode("MALFORMED_REQUEST")
                .path(getRequestPath(request))
                .build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    /**
     * Handles EntityNotFoundException and returns a 404 Not Found response.
     *
     * @param ex the exception to handle
     * @param request the web request
     * @return the response entity with the error response
     */
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleEntityNotFoundException(
            EntityNotFoundException ex, WebRequest request) {
        logger.warn("Entity not found: {}", ex.getMessage());
        ErrorResponse errorResponse = ErrorResponse.builder()
                .status(HttpStatus.NOT_FOUND.value())
                .message("Entity not found")
                .details(ex.getMessage())
                .timestamp(LocalDateTime.now())
                .errorCode("ENTITY_NOT_FOUND")
                .path(getRequestPath(request))
                .build();
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

    /**
     * Handles EmptyResultDataAccessException and returns a 404 Not Found response.
     *
     * @param ex the exception to handle
     * @param request the web request
     * @return the response entity with the error response
     */
    @ExceptionHandler(EmptyResultDataAccessException.class)
    public ResponseEntity<ErrorResponse> handleEmptyResultDataAccessException(
            EmptyResultDataAccessException ex, WebRequest request) {
        logger.warn("No result found: {}", ex.getMessage());
        ErrorResponse errorResponse = ErrorResponse.builder()
                .status(HttpStatus.NOT_FOUND.value())
                .message("Resource not found")
                .details("The requested resource does not exist")
                .timestamp(LocalDateTime.now())
                .errorCode("RESOURCE_NOT_FOUND")
                .path(getRequestPath(request))
                .build();
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

    /**
     * Handles DataIntegrityViolationException and returns a 409 Conflict response.
     *
     * @param ex the exception to handle
     * @param request the web request
     * @return the response entity with the error response
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolationException(
            DataIntegrityViolationException ex, WebRequest request) {
        logger.error("Data integrity violation: ", ex);
        final var details = getDetails(ex);
        ErrorResponse errorResponse = ErrorResponse.builder()
                .status(HttpStatus.CONFLICT.value())
                .message("Data integrity violation")
                .details(details)
                .timestamp(LocalDateTime.now())
                .errorCode("DATA_INTEGRITY_VIOLATION")
                .path(getRequestPath(request))
                .build();
        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
    }

    /**
     * Extracts the details from DataIntegrityViolationException.
     *
     * @param ex the exception to handle
     * @return the details
     */
    private static String getDetails(DataIntegrityViolationException ex) {
        String details = "A data integrity constraint was violated";
        if (ex.getCause() != null && ex.getCause().getMessage() != null) {
            String causeMessage = ex.getCause().getMessage();
            if (causeMessage.contains("unique constraint") || causeMessage.contains("Unique index")) {
                details = "A resource with this value already exists";
            } else if (causeMessage.contains("foreign key constraint")) {
                details = "Cannot perform this operation due to existing references";
            }
        }
        return details;
    }

    /**
     * Handles DataAccessException and returns a 500 Internal Server Error response.
     *
     * @param ex the exception to handle
     * @param request the web request
     * @return the response entity with the error response
     */
    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ErrorResponse> handleDataAccessException(
            DataAccessException ex, WebRequest request) {
        logger.error("Data access error: ", ex);
        ErrorResponse errorResponse = ErrorResponse.builder()
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .message("Database error")
                .details("An error occurred while accessing the database")
                .timestamp(LocalDateTime.now())
                .errorCode("DATA_ACCESS_ERROR")
                .path(getRequestPath(request))
                .build();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }

    /**
     * Handles GenericException and returns a 500 Internal Server Error response.
     *
     * @param ex the exception to handle
     * @param request the web request
     * @return the response entity with the error response
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception ex, WebRequest request) {
        logger.error("Unexpected error: ", ex);
        ErrorResponse errorResponse = ErrorResponse.builder()
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .message("Internal server error")
                .details("An unexpected error occurred while processing your request")
                .timestamp(LocalDateTime.now())
                .errorCode("INTERNAL_SERVER_ERROR")
                .path(getRequestPath(request))
                .build();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }

    /**
     * Extracts the request path from WebRequest.
     *
     * @param request the web request
     * @return the request path, or "unknown" if not available
     */
    private String getRequestPath(WebRequest request) {
        if (request instanceof ServletWebRequest) {
            return ((ServletWebRequest) request).getRequest().getRequestURI();
        }
        return "unknown";
    }
}
