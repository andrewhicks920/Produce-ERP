package com.andrewhicks.produce_erp.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.List;

/**
 * Represents a supplier — a vendor from whom the business purchases products.
 *
 * Suppliers sit at the top of the purchasing chain:
 *   Supplier → PurchaseOrder → PoLine → (on receipt) → Lot + InventoryTransaction
 *
 * A Supplier can have many PurchaseOrders over time. When goods are received
 * against those orders, Lots are created and linked back to the originating Supplier
 * for traceability (e.g. "which supplier did this batch come from?").
 *
 * Database table: supplier
 */
@Entity
@Table(name = "supplier")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Supplier {

    /** Primary key, auto-incremented by the database. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "supplier_id")
    private Long supplierId;

    /** The company or trading name of the supplier. */
    @Column(nullable = false)
    private String name;

    /** The name of the primary contact person at this supplier. */
    @Column(name = "contact_name")
    private String contactName;

    /**
     * All purchase orders raised against this supplier.
     * Lazily loaded to avoid fetching the full order history on every supplier lookup.
     */
    @OneToMany(mappedBy = "supplier", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<PurchaseOrder> purchaseOrders;

    /**
     * All inventory lots that originated from this supplier.
     * Used for supplier-level traceability (e.g. product recall management).
     */
    @OneToMany(mappedBy = "supplier", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Lot> lots;
}
