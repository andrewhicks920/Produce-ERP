package com.andrewhicks.produce_erp.repository;

import com.andrewhicks.produce_erp.model.InventoryTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Data access layer for InventoryTransaction entities.
 *
 * InventoryTransactions are the immutable audit log of all stock movements.
 * This repository is primarily used for read access — querying transaction history
 * by product, lot, originating document, or date range.
 *
 * Transactions are written by InventoryTransactionService.record(), which is
 * called from PurchaseOrderService (on receipt), SalesOrderService (on ship),
 * and InventoryAdjustmentService (on adjustment).
 *
 * Used by: InventoryTransactionService
 */
@Repository
public interface InventoryTransactionRepository extends JpaRepository<InventoryTransaction, Long> {

    /**
     * Full movement history for a product, newest first.
     * Used for the product stock ledger / transaction history view.
     */
    List<InventoryTransaction> findByProductProductIdOrderByTransactionDateDesc(Long productId);

    /**
     * Full movement history for a specific lot, newest first.
     * Used for lot-level traceability — "what happened to this batch?"
     */
    List<InventoryTransaction> findByLotLotIdOrderByTransactionDateDesc(Long lotId);

    /**
     * All transactions of a given type (RECEIPT, SALE, ADJUSTMENT, RETURN).
     * Useful for type-specific reporting (e.g. all sales transactions for COGS calculation).
     */
    List<InventoryTransaction> findByTransactionType(InventoryTransaction.TransactionType type);

    /**
     * All transactions linked to a specific source document.
     * e.g. findByReferenceTypeAndReferenceId(PURCHASE_ORDER, 42) returns all receipts
     * for PO #42, enabling drill-down from a PO into its stock movements.
     */
    List<InventoryTransaction> findByReferenceTypeAndReferenceId(
            InventoryTransaction.ReferenceType referenceType, Long referenceId);

    /**
     * All transactions within a date/time window.
     * Used for period-based reporting, stock reconciliation, and auditing.
     */
    List<InventoryTransaction> findByTransactionDateBetween(LocalDateTime from, LocalDateTime to);
}
