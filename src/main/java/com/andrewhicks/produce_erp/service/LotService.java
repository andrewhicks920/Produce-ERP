package com.andrewhicks.produce_erp.service;

import com.andrewhicks.produce_erp.exception.ResourceNotFoundException;
import com.andrewhicks.produce_erp.model.Lot;
import com.andrewhicks.produce_erp.model.Product;
import com.andrewhicks.produce_erp.model.Supplier;
import com.andrewhicks.produce_erp.repository.LotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Business logic layer for Lot management.
 *
 * Lots represent physical batches of stock. They are created when goods are received
 * (via PurchaseOrderService.receivePoLine()) and consumed during sales shipments
 * (via SalesOrderService.ship()). Manual adjustments can also modify lot quantities.
 *
 * This service is not directly exposed via a controller — lots are created as a
 * side effect of PO receipt and are queried via the ProductController's /lots endpoint.
 * adjustRemainingQuantity() is called internally by InventoryTransactionService
 * each time a transaction is posted against a lot.
 *
 * FIFO ordering is enforced at the repository level: findAvailableLotsByProduct
 * returns lots ordered by receivedDate ASC, so the caller always gets the oldest
 * lot first — see SalesOrderService.ship() for how this drives shipment.
 *
 * Called by:
 *   PurchaseOrderService.receivePoLine() — creates new lots on receipt
 *   SalesOrderService.ship() — reads available lots for FIFO selection
 *   InventoryTransactionService.record() — calls adjustRemainingQuantity after posting
 *   InventoryAdjustmentService.create() — validates lot references on adjustment lines
 */
@Service
@RequiredArgsConstructor
@Transactional
public class LotService {

    private final LotRepository lotRepository;
    private final ProductService productService;
    private final SupplierService supplierService;

    /**
     * Finds a lot by ID or throws ResourceNotFoundException (HTTP 404).
     * Used by other services to validate that a lotId refers to a real lot.
     */
    public Lot findById(Long id) {
        return lotRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Lot not found with id: " + id));
    }

    /**
     * Returns all lots for a product, ordered oldest-received-first.
     * Includes exhausted lots (remainingQuantity = 0) for full history.
     * Exposed via GET /api/products/{id}/lots
     */
    public List<Lot> findByProduct(Long productId) {
        return lotRepository.findByProductProductIdOrderByReceivedDateAsc(productId);
    }

    /**
     * Returns only lots with stock remaining for a product, ordered oldest-first.
     * This is the FIFO selection list used by SalesOrderService.ship().
     * Exposed via GET /api/products/{id}/lots?availableOnly=true
     */
    public List<Lot> findAvailableLotsByProduct(Long productId) {
        return lotRepository.findByProductProductIdAndRemainingQuantityGreaterThanOrderByReceivedDateAsc(productId, 0);
    }

    /**
     * Finds all lots expiring before a given date — used for expiry alerts.
     * E.g. call with LocalDate.now().plusDays(30) to find lots expiring within 30 days.
     */
    public List<Lot> findExpiringBefore(LocalDate date) {
        return lotRepository.findByExpirationDateBefore(date);
    }

    /**
     * Creates a new lot. Called by PurchaseOrderService.receivePoLine() when goods arrive.
     * Validates and resolves the product and supplier references, then sets
     * remainingQuantity equal to originalQuantity (the lot starts fully available).
     */
    public Lot create(Lot lot) {
        Product product = productService.findById(lot.getProduct().getProductId());
        lot.setProduct(product);
        if (lot.getSupplier() != null && lot.getSupplier().getSupplierId() != null) {
            Supplier supplier = supplierService.findById(lot.getSupplier().getSupplierId());
            lot.setSupplier(supplier);
        }
        lot.setRemainingQuantity(lot.getOriginalQuantity()); // lot starts fully available
        return lotRepository.save(lot);
    }

    /**
     * Updates the mutable metadata fields of an existing lot.
     * originalQuantity and receivedDate are intentionally not editable — they are
     * historical facts about the receipt event.
     */
    public Lot update(Long id, Lot updated) {
        Lot existing = findById(id);
        existing.setLotNumber(updated.getLotNumber());
        existing.setExpirationDate(updated.getExpirationDate());
        return lotRepository.save(existing);
    }

    /**
     * Adjusts the remainingQuantity of a lot by a signed delta.
     * Called by InventoryTransactionService.record() after posting every transaction.
     *
     * delta is positive for receipts (stock added), negative for sales and write-offs.
     * Does NOT validate that remainingQuantity stays >= 0 — the calling service
     * (SalesOrderService) is responsible for checking stock availability first.
     */
    public Lot adjustRemainingQuantity(Long lotId, int delta) {
        Lot lot = findById(lotId);
        lot.setRemainingQuantity(lot.getRemainingQuantity() + delta);
        return lotRepository.save(lot);
    }
}
