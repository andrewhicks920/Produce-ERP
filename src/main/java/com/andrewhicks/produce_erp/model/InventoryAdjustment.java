package com.andrewhicks.produce_erp.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Represents a manual inventory adjustment — a formal correction to stock levels
 * outside of the normal purchasing or sales flows.
 *
 * Common reasons for adjustments include:
 *   - Physical stocktake discrepancies (counted more or fewer units than the system shows)
 *   - Damaged or expired goods write-offs
 *   - Inter-warehouse transfers
 *   - Data corrections
 *
 * An adjustment is a header record that groups one or more AdjustmentLines.
 * When InventoryAdjustmentService.create() is called, it:
 *   1. Saves the adjustment and all its lines
 *   2. Posts an InventoryTransaction (type ADJUSTMENT) for each line,
 *      which in turn updates the affected Lot's remainingQuantity
 *
 * Adjustments are identified by referenceType=ADJUSTMENT + the adjustmentId
 * in any InventoryTransaction records they generate.
 *
 * Database table: inventory_adjustment
 */
@Entity
@Table(name = "inventory_adjustment")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class InventoryAdjustment {

    /** Primary key, auto-incremented by the database. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "adjustment_id")
    private Long adjustmentId;

    /** A description of why this adjustment was made (e.g. "Annual stocktake Q4"). */
    @Column(nullable = false)
    private String reason;

    /** The username or identifier of the person who created this adjustment. */
    @Column(name = "created_by", nullable = false)
    private String createdBy;

    /** Timestamp of when the adjustment was created. Set automatically by the service. */
    @Column(name = "created_date", nullable = false)
    private LocalDateTime createdDate;

    /**
     * The individual lot-level corrections that make up this adjustment.
     * CascadeType.ALL means lines are saved and deleted with the adjustment header.
     */
    @OneToMany(mappedBy = "inventoryAdjustment", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<AdjustmentLine> adjustmentLines;
}
