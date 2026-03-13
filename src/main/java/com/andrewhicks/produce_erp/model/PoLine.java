package com.andrewhicks.produce_erp.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * Represents a single line item within a Purchase Order.
 *
 * Each PoLine specifies:
 *   - Which product is being ordered
 *   - How many units were ordered (orderedQuantity)
 *   - The agreed cost per unit at time of ordering (unitCost)
 *   - How many units have been received so far (receivedQuantity)
 *
 * When goods are received via PurchaseOrderService.receivePoLine(), the
 * receivedQuantity is incremented. If it reaches orderedQuantity, the parent
 * PurchaseOrder status is updated to RECEIVED. The unitCost from the PoLine
 * is carried forward to the new Lot, enabling FIFO cost tracking.
 *
 * Database table: po_line
 */
@Entity
@Table(name = "po_line")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PoLine {

    /** Primary key, auto-incremented by the database. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "po_line_id")
    private Long poLineId;

    /**
     * The parent purchase order this line belongs to.
     * Many lines can belong to one PurchaseOrder.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "po_id", nullable = false)
    private PurchaseOrder purchaseOrder;

    /**
     * The product being ordered on this line.
     * Many PoLines can reference the same Product across different POs.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    /** The total quantity of this product requested in the order. */
    @Column(name = "ordered_quantity", nullable = false)
    private Integer orderedQuantity;

    /** The agreed purchase price per unit, stored with 2 decimal places. */
    @Column(name = "unit_cost", nullable = false, precision = 10, scale = 2)
    private BigDecimal unitCost;

    /**
     * Running count of how many units have been received so far.
     * Starts at 0 and is incremented each time a receipt is posted against this line.
     */
    @Column(name = "received_quantity")
    private Integer receivedQuantity = 0;
}
