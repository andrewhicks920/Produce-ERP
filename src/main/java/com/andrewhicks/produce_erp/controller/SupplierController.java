package com.andrewhicks.produce_erp.controller;

import com.andrewhicks.produce_erp.model.Supplier;
import com.andrewhicks.produce_erp.service.SupplierService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for Supplier management.
 *
 * Exposes CRUD endpoints for Supplier entities. Suppliers are the vendors
 * from whom the business purchases products — they feed into the Purchase Order flow.
 *
 * Base URL: /api/suppliers
 *
 * Endpoints:
 *   GET    /api/suppliers          → list all (optionally filter by name)
 *   GET    /api/suppliers/{id}     → get one supplier
 *   POST   /api/suppliers          → create a new supplier
 *   PUT    /api/suppliers/{id}     → update an existing supplier
 *   DELETE /api/suppliers/{id}     → delete a supplier
 *
 * All business logic and validation is delegated to SupplierService.
 * Errors (404, 400) are handled globally by GlobalExceptionHandler.
 */
@RestController
@RequestMapping("/api/suppliers")
@RequiredArgsConstructor
public class SupplierController {

    private final SupplierService supplierService;

    /**
     * Returns all suppliers, or filters by name if the ?name= query param is provided.
     * Example: GET /api/suppliers?name=acme
     */
    @GetMapping
    public ResponseEntity<List<Supplier>> findAll(@RequestParam(required = false) String name) {
        if (name != null && !name.isBlank()) {
            return ResponseEntity.ok(supplierService.searchByName(name));
        }
        return ResponseEntity.ok(supplierService.findAll());
    }

    /** Returns a single supplier by its ID. Returns 404 if not found. */
    @GetMapping("/{id}")
    public ResponseEntity<Supplier> findById(@PathVariable Long id) {
        return ResponseEntity.ok(supplierService.findById(id));
    }

    /** Creates a new supplier. Returns the created supplier with HTTP 201. */
    @PostMapping
    public ResponseEntity<Supplier> create(@RequestBody Supplier supplier) {
        return ResponseEntity.status(HttpStatus.CREATED).body(supplierService.create(supplier));
    }

    /** Updates name and contactName of an existing supplier. Returns the updated record. */
    @PutMapping("/{id}")
    public ResponseEntity<Supplier> update(@PathVariable Long id, @RequestBody Supplier supplier) {
        return ResponseEntity.ok(supplierService.update(id, supplier));
    }

    /** Deletes a supplier. Returns HTTP 204 No Content on success. */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        supplierService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
