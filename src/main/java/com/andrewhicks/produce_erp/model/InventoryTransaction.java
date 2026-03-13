package com.andrewhicks.produce_erp.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Represents a single stock movement — the immutable audit log of all inventory changes.
 *
 * Every time stock is added or removed, an InventoryTransaction is created. This is the
 * system's source of truth for what happened, when, and at what cost. Transactions are
 * never deleted or modified — they form a permanent ledger.
 *
 * Three sources create transactions:
 *   1. PurchaseOrderService.receivePoLine() → type RECEIPT, quantityChange positive
 *   2. SalesOrderService.ship()             → type SALE,    quantityChange negative
 *   3. InventoryAdjustmentService.create()  → type ADJUSTMENT, quantityChange positive or negative
 *
 * Each transaction is linked to both a Product (what moved) and a Lot (which batch),
 * enabling full lot-level traceability. The referenceType + referenceId pair points
 * back to the originating business document (PO, Sales Order, or Adjustment).
 *
 * Posting a transaction also calls LotService.adjustRemainingQuantity() to keep
 * the Lot's remainingQuantity in sync with actual stock movements.
 *
 * Database table: inventory_transaction
 */
@Entity
@Table(name = "inventory_transaction")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class InventoryTransaction {

    /** Primary key, auto-incremented by the database. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "transaction_id")
    private Long transactionId;

    /**
     * The product whose stock is being changed.
     * Denormalized from the Lot for easier querying of all movements per product.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    /**
     * The specific lot from which stock is being added or removed.
     * Enables lot-level traceability for every movement.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lot_id", nullable = false)
    private Lot lot;

    /** The nature of this stock movement — see TransactionType for details. */
    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false)
    private TransactionType transactionType;

    /**
     * The quantity change applied to the lot.
     * Positive for stock additions (receipts), negative for reductions (sales, write-offs).
     */
    @Column(name = "quantity_change", nullable = false)
    private Integer quantityChange;

    /**
     * The cost per unit at the time of this movement.
     * For RECEIPTs, this is copied from the PoLine. For SALEs, this is the lot's
     * unitCost at the time of shipment, enabling accurate FIFO COGS calculation.
     */
    @Column(name = "unit_cost", precision = 10, scale = 2)
    private BigDecimal unitCost;

    /** The type of document that triggered this transaction. */
    @Enumerated(EnumType.STRING)
    @Column(name = "reference_type")
    private ReferenceType referenceType;

    /**
     * The primary key of the originating document.
     * Combine with referenceType to look up the source:
     *   PURCHASE_ORDER → PurchaseOrder.poId
     *   SALES_ORDER    → SalesOrder.salesOrderId
     *   ADJUSTMENT     → InventoryAdjustment.adjustmentId
     */
    @Column(name = "reference_id")
    private Long referenceId;

    /** Timestamp of when this transaction was posted. Set automatically on creation. */
    @Column(name = "transaction_date", nullable = false)
    private LocalDateTime transactionDate;

    /** The type of stock movement this transaction represents. */
    public enum TransactionType {
        /** Stock added to inventory from a supplier delivery. */
        RECEIPT,
        /** Stock removed from inventory to fulfill a customer sales order. */
        SALE,
        /** Manual stock correction (counted more or fewer units than expected). */
        ADJUSTMENT,
        /** Stock returned to inventory from a customer. */
        RETURN
    }

    /** The category of document that originated this transaction. */
    public enum ReferenceType {
        PURCHASE_ORDER,
        SALES_ORDER,
        ADJUSTMENT
    }
}
