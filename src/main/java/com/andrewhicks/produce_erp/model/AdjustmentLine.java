package com.andrewhicks.produce_erp.model;

import jakarta.persistence.*;
import lombok.*;

/**
 * Represents a single lot-level correction within an InventoryAdjustment.
 *
 * Each AdjustmentLine targets a specific Product + Lot combination and specifies
 * the quantity_change to apply (positive to add stock, negative to remove it).
 *
 * When the parent InventoryAdjustment is saved via InventoryAdjustmentService.create(),
 * each AdjustmentLine triggers:
 *   1. An InventoryTransaction posted against the specified lot
 *   2. An update to Lot.remainingQuantity via LotService.adjustRemainingQuantity()
 *
 * Unlike sales (which select lots automatically via FIFO), adjustments specify
 * the exact lot to correct — this is intentional, since adjustments are precise
 * manual interventions requiring the operator to know exactly which batch to fix.
 *
 * Database table: adjustment_line
 */
@Entity
@Table(name = "adjustment_line")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AdjustmentLine {

    /** Primary key, auto-incremented by the database. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "adjustment_line_id")
    private Long adjustmentLineId;

    /**
     * The parent adjustment header this line belongs to.
     * Many lines can belong to one InventoryAdjustment.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "adjustment_id", nullable = false)
    private InventoryAdjustment inventoryAdjustment;

    /** The product whose stock is being adjusted on this line. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    /**
     * The specific lot to adjust.
     * Must belong to the specified product. The operator selects this explicitly
     * so that the correction is applied to exactly the right inventory batch.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lot_id", nullable = false)
    private Lot lot;

    /**
     * The quantity change to apply to this lot.
     * Positive values increase stock (e.g. found units during stocktake).
     * Negative values decrease stock (e.g. damaged goods write-off).
     */
    @Column(name = "quantity_change", nullable = false)
    private Integer quantityChange;
}
