package com.andrewhicks.produce_erp.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;


/**
 * Represents a Purchase Order (PO), or a formal request to buy products from a Supplier.
 *
 * A PurchaseOrder is the starting point for bringing new stock into the system:
 * Supplier → PurchaseOrder → PoLine → (on receipt) → Lot + InventoryTransaction
 *
 * The PO tracks which supplier is being ordered from, when the order was placed,
 * and when goods are expected. Its status progresses through a lifecycle:
 *   DRAFT → SENT → PARTIALLY_RECEIVED → RECEIVED (or CANCELLED)
 *
 * When goods are received against a PO line (via PurchaseOrderService.receivePoLine()),
 * a new Lot is created and an InventoryTransaction of type RECEIPT is posted,
 * which physically adds stock to the system.
 */
@Entity
@Table(name = "purchase_order")
@Data
public class PurchaseOrder {

    // Possible states a PurchaseOrder can be in throughout its lifecycle
    public enum PurchaseOrderStatus {
        DRAFT, // Created but not yet sent to the supplier
        SENT, // Sent to the supplier and awaiting delivery
        PARTIALLY_RECEIVED, // Some lines have been received but the order is not yet complete
        RECEIVED, // All ordered quantities have been received
        CANCELLED // The order was cancelled before full receipt
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /*
    Why use LAZY:

    Often a PO listing or operations (like printing orders) only need basic info
    from the PO (like orderDate, status) and don’t need full Supplier details
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id", nullable = false)
    private Supplier supplier;

    @Column(name = "order_date")
    private LocalDate orderDate;

    @Column(name = "expected_date")
    private LocalDate expectedDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PurchaseOrderStatus status;


}
