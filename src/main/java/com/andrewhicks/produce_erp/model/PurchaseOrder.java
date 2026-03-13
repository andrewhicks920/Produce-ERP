package com.andrewhicks.produce_erp.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.List;

/**
 * Represents a Purchase Order (PO) — a formal request to buy products from a Supplier.
 *
 * A PurchaseOrder is the starting point for bringing new stock into the system:
 *   Supplier → PurchaseOrder → PoLine → (on receipt) → Lot + InventoryTransaction
 *
 * The PO tracks which supplier is being ordered from, when the order was placed,
 * and when goods are expected. Its status progresses through a lifecycle:
 *   DRAFT → SENT → PARTIALLY_RECEIVED → RECEIVED (or CANCELLED)
 *
 * When goods are received against a PO line (via PurchaseOrderService.receivePoLine()),
 * a new Lot is created and an InventoryTransaction of type RECEIPT is posted,
 * which physically adds stock to the system.
 *
 * Database table: purchase_order
 */
@Entity
@Table(name = "purchase_order")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PurchaseOrder {

    /** Primary key, auto-incremented by the database. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "po_id")
    private Long poId;

    /**
     * The supplier this order is placed with.
     * Many POs can belong to one Supplier.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id", nullable = false)
    private Supplier supplier;

    /** The date the purchase order was created or submitted. */
    @Column(name = "order_date", nullable = false)
    private LocalDate orderDate;

    /** The date by which the supplier is expected to deliver the goods. */
    @Column(name = "expected_date")
    private LocalDate expectedDate;

    /**
     * Lifecycle status of this purchase order.
     * Updated automatically by PurchaseOrderService as lines are received.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PurchaseOrderStatus status;

    /**
     * The individual line items on this order, each referencing a product and quantity.
     * CascadeType.ALL means lines are saved/deleted with the PO.
     */
    @OneToMany(mappedBy = "purchaseOrder", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<PoLine> poLines;

    /** Possible states a PurchaseOrder can be in throughout its lifecycle. */
    public enum PurchaseOrderStatus {
        /** Created but not yet sent to the supplier. */
        DRAFT,
        /** Sent to the supplier and awaiting delivery. */
        SENT,
        /** Some lines have been received but the order is not yet complete. */
        PARTIALLY_RECEIVED,
        /** All ordered quantities have been received. */
        RECEIVED,
        /** The order was cancelled before full receipt. */
        CANCELLED
    }
}
