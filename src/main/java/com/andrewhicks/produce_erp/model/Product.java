package com.andrewhicks.produce_erp.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.List;

/**
 * Represents a product — a distinct item that the business buys, stocks, and sells.
 *
 * Products are the central entity of the inventory system. They connect the
 * purchasing side (PoLine) to the sales side (SalesOrderLine), with stock
 * physically tracked via Lots and InventoryTransactions.
 *
 * Key relationships:
 *   Product ← PoLine         (what is being ordered)
 *   Product ← SalesOrderLine (what is being sold)
 *   Product → Lot            (physical batches of stock on hand)
 *   Product → InventoryTransaction (every stock movement for this product)
 *
 * The costMethod field (currently always FIFO) determines how cost-of-goods-sold
 * is calculated when stock is consumed. Under FIFO, the oldest lots are consumed
 * first — see SalesOrderService.ship() for the implementation.
 *
 * Database table: product
 */
@Entity
@Table(name = "product")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Product {

    /** Primary key, auto-incremented by the database. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "product_id")
    private Long productId;

    /** The display name of the product (e.g. "Widget A"). */
    @Column(nullable = false)
    private String name;

    /**
     * Stock Keeping Unit — a unique identifier for this product used in
     * warehousing and order management. Enforced unique at the database level.
     */
    @Column(unique = true, nullable = false)
    private String sku;

    /** Grouping category for reporting and filtering (e.g. "Electronics", "Raw Materials"). */
    private String category;

    /**
     * The unit in which this product is measured (e.g. "EA" for each, "KG", "L").
     * Used to label quantities throughout the system.
     */
    @Column(name = "unit_of_measure")
    private String unitOfMeasure;

    /**
     * The inventory costing method applied to this product.
     * Currently only FIFO is supported — lots are consumed oldest-first.
     * This affects which lot's unitCost is used when recording sales transactions.
     */
    @Column(name = "cost_method")
    @Enumerated(EnumType.STRING)
    private CostMethod costMethod = CostMethod.FIFO;

    /**
     * All physical inventory batches (lots) currently or previously held for this product.
     * To get available stock, filter by remainingQuantity > 0.
     */
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Lot> lots;

    /**
     * Full transaction history for this product — every receipt, sale, and adjustment.
     * Useful for auditing stock movements and reconciling discrepancies.
     */
    @OneToMany(mappedBy = "product", fetch = FetchType.LAZY)
    private List<InventoryTransaction> inventoryTransactions;

    /** Supported inventory costing methods. */
    public enum CostMethod {
        /** First-In, First-Out: oldest lots are consumed before newer ones. */
        FIFO
    }
}
