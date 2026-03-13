package com.andrewhicks.produce_erp.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown when a requested entity does not exist in the database.
 *
 * Examples:
 *   - GET /api/products/999 and product 999 doesn't exist
 *   - Attempting to create a PO with a supplierId that doesn't exist
 *
 * @ResponseStatus(NOT_FOUND) means Spring will return HTTP 404 automatically
 * if this exception bubbles up past the controller. In this project, it is
 * intercepted by GlobalExceptionHandler for a consistent JSON error response.
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
