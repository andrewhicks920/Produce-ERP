package com.andrewhicks.produce_erp.repository;

import com.andrewhicks.produce_erp.model.Lot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;

/**
 * Data access layer for Lot entities.
 *
 * Lots are the physical inventory batches that underpin both cost tracking
 * and stock availability. This repository contains the FIFO ordering logic
 * that the rest of the system depends on for correct lot selection during sales.
 *
 * Used by: LotService, which is called by PurchaseOrderService (on receipt)
 *          and SalesOrderService (on shipment) and InventoryAdjustmentService.
 */
@Repository
public interface LotRepository extends JpaRepository<Lot, Long> {

    /**
     * Returns ALL lots for a product, ordered oldest-first by received date.
     * This is the FIFO ordering — the first lot in the list should be consumed first.
     * Used for displaying lot history and by the FIFO selection logic.
     */
    List<Lot> findByProductProductIdOrderByReceivedDateAsc(Long productId);

    /**
     * Returns only lots with stock remaining (remainingQuantity > qty, where qty = 0),
     * ordered oldest-first. This is the key query used during SalesOrderService.ship()
     * to select which lots to deduct from — driving the FIFO consumption algorithm.
     */
    List<Lot> findByProductProductIdAndRemainingQuantityGreaterThanOrderByReceivedDateAsc(Long productId, int qty);

    /**
     * Returns all lots expiring before a given date.
     * Used for expiry alerts — e.g. "show me everything expiring in the next 30 days".
     */
    List<Lot> findByExpirationDateBefore(LocalDate date);

    /**
     * Calculates total on-hand stock for a product by summing remainingQuantity across all its lots.
     * Returns null (not 0) if no lots exist — callers should handle this with a null check.
     * Used by ProductService.getTotalStock() and exposed via GET /api/products/{id}/stock.
     */
    @Query("SELECT SUM(l.remainingQuantity) FROM Lot l WHERE l.product.productId = :productId")
    Integer getTotalStockByProduct(Long productId);
}
