package com.andrewhicks.produce_erp.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * Represents a single line item within a Sales Order.
 *
 * Each SalesOrderLine specifies which product the customer wants and how many units,
 * along with the agreed selling price per unit. Multiple lines together make up
 * the full contents of a SalesOrder.
 *
 * During shipment (SalesOrderService.ship()), each SalesOrderLine drives the
 * FIFO lot consumption logic: the service looks up available lots for the product
 * and deducts the requested quantity oldest-lot-first, posting InventoryTransactions
 * for each lot consumed.
 *
 * Database table: sales_order_line
 */
@Entity
@Table(name = "sales_order_line")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SalesOrderLine {

    /** Primary key, auto-incremented by the database. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "so_line_id")
    private Long soLineId;

    /**
     * The parent sales order this line belongs to.
     * Many lines can belong to one SalesOrder.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sales_order_id", nullable = false)
    private SalesOrder salesOrder;

    /**
     * The product the customer is purchasing on this line.
     * Used during shipment to look up available lots via LotRepository.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    /** The number of units of this product the customer wants. */
    @Column(nullable = false)
    private Integer quantity;

    /** The agreed selling price per unit, stored with 2 decimal places. */
    @Column(name = "unit_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPrice;
}
