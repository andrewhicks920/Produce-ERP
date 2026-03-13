package com.andrewhicks.produce_erp.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown when a request is structurally valid but violates a business rule.
 *
 * This is distinct from ResourceNotFoundException (entity not found) and from
 * validation errors (malformed request body). BusinessException covers domain
 * logic failures such as:
 *   - Trying to edit a PurchaseOrder that is no longer in DRAFT status
 *   - Trying to ship a SalesOrder with insufficient stock
 *   - Receiving more units than were ordered on a PO line
 *   - Adding a payment to an already-paid or cancelled invoice
 *   - Creating a product with a duplicate SKU
 *
 * Intercepted by GlobalExceptionHandler and returned as HTTP 400 Bad Request
 * with a descriptive message.
 */
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class BusinessException extends RuntimeException {
    public BusinessException(String message) {
        super(message);
    }
}
