package com.andrewhicks.produce_erp.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Represents a Sales Order — a customer's request to purchase products.
 *
 * A SalesOrder is the starting point of the outbound fulfilment flow:
 *   Customer → SalesOrder → SalesOrderLine → (ship) → InventoryTransaction (SALE)
 *                        → Invoice → Payment
 *
 * Status lifecycle:
 *   DRAFT → CONFIRMED → PARTIALLY_SHIPPED → SHIPPED → INVOICED (or CANCELLED)
 *
 * Shipping is triggered by SalesOrderService.ship(), which performs FIFO lot
 * consumption across all lines: it walks the SalesOrderLines, finds available
 * Lots ordered by received date (oldest first), and posts InventoryTransactions
 * to reduce stock. If there is insufficient stock for any line, the entire
 * shipment is rejected with a BusinessException.
 *
 * After shipping, one or more Invoices are typically raised against the order,
 * and Payments are applied until the invoice is fully paid.
 *
 * Database table: sales_order
 */
@Entity
@Table(name = "sales_order")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SalesOrder {

    /** Primary key, auto-incremented by the database. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "sales_order_id")
    private Long salesOrderId;

    /**
     * The customer who placed this order.
     * Many SalesOrders can belong to one Customer.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    /** The date the order was placed. Defaults to today in SalesOrderService.create(). */
    @Column(name = "order_date", nullable = false)
    private LocalDate orderDate;

    /**
     * Lifecycle status of the order.
     * Updated by SalesOrderService as the order progresses through fulfilment.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SalesOrderStatus status;

    /**
     * The total value of the order (sum of line quantity × unitPrice).
     * Calculated automatically in SalesOrderService.create().
     */
    @Column(name = "total_amount", precision = 12, scale = 2)
    private BigDecimal totalAmount;

    /**
     * The individual line items specifying which products and quantities are ordered.
     * CascadeType.ALL means lines are saved and deleted with the order.
     */
    @OneToMany(mappedBy = "salesOrder", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<SalesOrderLine> salesOrderLines;

    /**
     * Invoices raised against this sales order.
     * A single order may be invoiced in multiple parts (partial invoicing).
     */
    @OneToMany(mappedBy = "salesOrder", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Invoice> invoices;

    /** Possible states a SalesOrder can be in throughout its lifecycle. */
    public enum SalesOrderStatus {
        /** Created but not yet confirmed by the business. */
        DRAFT,
        /** Confirmed and ready to be picked and shipped. */
        CONFIRMED,
        /** Some lines have shipped but the order is not fully fulfilled. */
        PARTIALLY_SHIPPED,
        /** All lines have been shipped to the customer. */
        SHIPPED,
        /** An invoice has been raised for the shipped goods. */
        INVOICED,
        /** The order was cancelled before shipment. */
        CANCELLED
    }
}
