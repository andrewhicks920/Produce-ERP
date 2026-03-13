package com.andrewhicks.produce_erp.controller;

import com.andrewhicks.produce_erp.model.Customer;
import com.andrewhicks.produce_erp.service.CustomerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for Customer management.
 *
 * Customers are the buyers who place sales orders. This controller provides
 * standard CRUD endpoints for managing customer records.
 *
 * Base URL: /api/customers
 *
 * Endpoints:
 *   GET    /api/customers          → list all (optionally filter by ?name=)
 *   GET    /api/customers/{id}     → get one customer
 *   POST   /api/customers          → create a new customer
 *   PUT    /api/customers/{id}     → update an existing customer
 *   DELETE /api/customers/{id}     → delete a customer
 *
 * All business logic is delegated to CustomerService.
 * Errors (404, 400) are handled globally by GlobalExceptionHandler.
 */
@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;

    /**
     * Returns all customers, or filters by name if the ?name= query param is provided.
     * Example: GET /api/customers?name=john
     */
    @GetMapping
    public ResponseEntity<List<Customer>> findAll(@RequestParam(required = false) String name) {
        if (name != null && !name.isBlank()) {
            return ResponseEntity.ok(customerService.searchByName(name));
        }
        return ResponseEntity.ok(customerService.findAll());
    }

    /** Returns a single customer by ID. Returns 404 if not found. */
    @GetMapping("/{id}")
    public ResponseEntity<Customer> findById(@PathVariable Long id) {
        return ResponseEntity.ok(customerService.findById(id));
    }

    /** Creates a new customer. Returns the created customer with HTTP 201. */
    @PostMapping
    public ResponseEntity<Customer> create(@RequestBody Customer customer) {
        return ResponseEntity.status(HttpStatus.CREATED).body(customerService.create(customer));
    }

    /** Updates the name of an existing customer. Returns the updated record. */
    @PutMapping("/{id}")
    public ResponseEntity<Customer> update(@PathVariable Long id, @RequestBody Customer customer) {
        return ResponseEntity.ok(customerService.update(id, customer));
    }

    /** Deletes a customer. Returns HTTP 204 No Content on success. */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        customerService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
