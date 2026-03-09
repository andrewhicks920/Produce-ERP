package com.andrewhicks.produce_erp.service;

import com.andrewhicks.produce_erp.model.Supplier;
import com.andrewhicks.produce_erp.repository.SupplierRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Business logic layer for Supplier management.
 *
 * Suppliers are the vendors that supply products via Purchase Orders. This service
 * handles CRUD operations and is also called by other services that need to
 * resolve a supplierId into a full Supplier entity before associating it with
 * a PurchaseOrder or Lot.
 *
 * Called by:
 *   SupplierController (via REST API)
 *   PurchaseOrderService.create() — to validate and attach the supplier
 *   LotService.create() — to validate and attach the supplier to new lots
 */
@Service
@RequiredArgsConstructor
public class SupplierService {
    private final SupplierRepository supplierRepository;

    public List<Supplier> getAllSuppliers() {
        return supplierRepository.findAll();
    }

    public Supplier getSupplierById(Long id) {
        return supplierRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Supplier not found with id: " + id));
    }

    // Case-insensitive name search. Returns all matches
    public List<Supplier> searchByName(String name) {
        return supplierRepository.findByNameContainingIgnoreCase(name);
    }

    // Persists a new supplier. No uniqueness constraint enforced
    public Supplier createSupplier(Supplier supplier) {
        return supplierRepository.save(supplier);
    }

    // Updates the name and contact name of an existing supplier.
    // Fetches the existing record first to ensure it exists, then applies changes.
    public Supplier updateSupplier(Long id, Supplier updated) {
        Supplier existing = getSupplierById(id);

        existing.setName(updated.getName());
        existing.setContactName(updated.getContactName());
        return supplierRepository.save(existing);
    }

    /**
     * Deletes a supplier by ID.
     * Note: if the supplier has associated PurchaseOrders or Lots, the database
     * will throw a constraint violation. Add a check here if soft-deletes are needed.
     */
    public void deleteSupplier(Long id) {
        getSupplierById(id);
        supplierRepository.deleteById(id);
    }
}
