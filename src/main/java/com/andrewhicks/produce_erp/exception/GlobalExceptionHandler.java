package com.andrewhicks.produce_erp.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Centralized exception handler for all REST controllers.
 *
 * Without this class, Spring would return its default HTML error pages or
 * generic JSON error structures. This handler intercepts all exceptions thrown
 * from any @RestController in the project and returns a consistent JSON format:
 *
 * {
 *   "timestamp": "2024-01-15T10:30:00",
 *   "status": 404,
 *   "error": "Not Found",
 *   "message": "Product not found with id: 99"
 * }
 *
 * Exception routing:
 *   ResourceNotFoundException → 404 Not Found
 *   BusinessException         → 400 Bad Request (violated business rule)
 *   MethodArgumentNotValidException → 400 Bad Request (invalid request body fields)
 *   Any other Exception       → 500 Internal Server Error (unexpected failures)
 *
 * @RestControllerAdvice applies this handler globally to all controllers in the application.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handles requests for entities that do not exist (e.g. GET /api/products/999).
     * Returns HTTP 404.
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(ResourceNotFoundException ex) {
        return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    /**
     * Handles business rule violations (e.g. shipping an order with no stock).
     * Returns HTTP 400.
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Map<String, Object>> handleBusiness(BusinessException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    /**
     * Handles @Valid/@Validated failures on @RequestBody fields.
     * Collects all field-level validation errors into a single comma-separated message.
     * Returns HTTP 400.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .reduce("", (a, b) -> a.isEmpty() ? b : a + ", " + b);
        return buildResponse(HttpStatus.BAD_REQUEST, message);
    }

    /**
     * Catch-all for any unexpected exception not handled above.
     * Returns HTTP 500 without exposing internal details.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneral(Exception ex) {
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred");
    }

    /** Builds the standard error response body used by all handlers. */
    private ResponseEntity<Map<String, Object>> buildResponse(HttpStatus status, String message) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now().toString());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", message);
        return new ResponseEntity<>(body, status);
    }
}
