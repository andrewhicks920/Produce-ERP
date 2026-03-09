package com.andrewhicks.produce_erp.controller;

import com.andrewhicks.produce_erp.model.Supplier;
import com.andrewhicks.produce_erp.service.SupplierService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for Supplier management. (note this Javadoc documentation
 * will be added to other Controller files in future — I'm just lazy right now)
 *
 * Base URL: /suppliers
 *
 * Endpoints:
 *   GET    /suppliers          → List all (optionally filter by name)
 *   GET    /suppliers/{id}     → Get a Supplier by ID (1 return)
 *   POST   /suppliers          → Create a new Supplier
 *   PUT    /suppliers/{id}     → Update an existing Supplier
 *   DELETE /suppliers/{id}     → Delete a Supplier
 */
@RestController
@RequiredArgsConstructor // Basically injects public SupplierController(SupplierService service) {this.service = service;} w/o having to write it
@RequestMapping("/suppliers")
public class SupplierController {
    private final SupplierService supplierService; // But using this I have to declare this as final

    @GetMapping
    public List<Supplier> getAllSuppliers() {
        return supplierService.getAllSuppliers();
    }

    @GetMapping("/{id}")
    public Supplier getSupplierByID(@PathVariable Long id) {
        return supplierService.getSupplierById(id);
    }

    @PostMapping
    public Supplier createSupplier(@RequestBody Supplier supplier) {
        return supplierService.createSupplier(supplier);
    }

    // Updates name & contactName of existing Supplier
    @PutMapping("/{id}")
    public Supplier updateSupplierByID(@PathVariable Long id, @RequestBody Supplier supplier) {
        return supplierService.updateSupplier(id, supplier);
    }

    @DeleteMapping
    public void deleteSupplierByID(@PathVariable Long id) {
        supplierService.deleteSupplier(id);
    }






}
