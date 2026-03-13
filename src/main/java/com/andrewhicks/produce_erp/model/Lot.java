package com.andrewhicks.produce_erp.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Represents a Lot — a physical batch of a product received into inventory.
 *
 * Lots are the core unit of inventory tracking in this system. Every time goods
 * are received against a Purchase Order line, a new Lot is created capturing:
 *   - Which product it is
 *   - Which supplier it came from
 *   - A lot/batch number (for traceability)
 *   - How many units were received (originalQuantity)
 *   - How many remain unused (remainingQuantity)
 *   - The cost per unit at time of receipt (unitCost, copied from PoLine)
 *   - Expiration date if applicable (for perishables, pharmaceuticals, etc.)
 *
 * FIFO ordering: Lots are ordered by receivedDate ascending in LotRepository,
 * so the oldest lots are consumed first when fulfilling sales orders.
 * See SalesOrderService.ship() for how FIFO is implemented during shipment.
 *
 * remainingQuantity is decremented by InventoryTransactionService.record()
 * whenever a SALE or ADJUSTMENT transaction is posted against this lot.
 *
 * Database table: lot
 */
@Entity
@Table(name = "lot")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Lot {

    /** Primary key, auto-incremented by the database. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "lot_id")
    private Long lotId;

    /**
     * The product this lot contains.
     * Multiple lots can exist for the same product (each receipt creates a new lot).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    /**
     * The supplier this lot was purchased from.
     * Enables traceability back to source for quality control and recalls.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id")
    private Supplier supplier;

    /**
     * The supplier's batch or lot reference number.
     * Used for traceability — particularly important for regulated industries.
     */
    @Column(name = "lot_number", nullable = false)
    private String lotNumber;

    /** The date this lot was physically received into the warehouse. Used for FIFO ordering. */
    @Column(name = "received_date", nullable = false)
    private LocalDate receivedDate;

    /** Optional expiry date. Null if the product does not expire. */
    @Column(name = "expiration_date")
    private LocalDate expirationDate;

    /** The total quantity received when this lot was created. Never changes after creation. */
    @Column(name = "original_quantity", nullable = false)
    private Integer originalQuantity;

    /**
     * How many units of this lot remain available.
     * Starts equal to originalQuantity and is decremented as stock is consumed.
     * When this reaches 0, the lot is exhausted and will be skipped in FIFO selection.
     */
    @Column(name = "remaining_quantity", nullable = false)
    private Integer remainingQuantity;

    /**
     * The cost per unit for this specific lot, inherited from the PoLine at receipt time.
     * This value is used in InventoryTransactions to record the FIFO cost of each movement.
     */
    @Column(name = "unit_cost", nullable = false, precision = 10, scale = 2)
    private BigDecimal unitCost;

    /**
     * All inventory movements (sales, adjustments) that have debited this lot.
     * Together with originalQuantity, these explain how remainingQuantity arrived at its value.
     */
    @OneToMany(mappedBy = "lot", fetch = FetchType.LAZY)
    private List<InventoryTransaction> inventoryTransactions;
}
