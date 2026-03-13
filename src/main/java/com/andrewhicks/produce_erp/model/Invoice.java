package com.andrewhicks.produce_erp.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Represents an Invoice — a financial document sent to a customer requesting payment
 * for goods delivered on a Sales Order.
 *
 * Invoices sit between the fulfilment and payment sides of the sales flow:
 *   SalesOrder → Invoice → Payment
 *
 * An invoice is typically created after a SalesOrder has been shipped (status SHIPPED).
 * A single SalesOrder may generate multiple invoices (e.g. partial invoicing or
 * re-invoicing after disputes), though a single invoice per order is the common case.
 *
 * Status lifecycle:
 *   DRAFT → SENT → PARTIALLY_PAID → PAID (or CANCELLED)
 *
 * The status is updated automatically by InvoiceService.addPayment() as payments
 * are applied: once the sum of all payments equals or exceeds totalAmount,
 * the invoice is marked PAID.
 *
 * Database table: invoice
 */
@Entity
@Table(name = "invoice")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Invoice {

    /** Primary key, auto-incremented by the database. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "invoice_id")
    private Long invoiceId;

    /**
     * The sales order this invoice is billing for.
     * Many invoices can reference the same SalesOrder (partial invoicing scenarios).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sales_order_id", nullable = false)
    private SalesOrder salesOrder;

    /** The date this invoice was issued. Defaults to today in InvoiceService.create(). */
    @Column(name = "invoice_date", nullable = false)
    private LocalDate invoiceDate;

    /** The total amount owed by the customer for this invoice. */
    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;

    /**
     * Current payment status of this invoice.
     * Automatically updated by InvoiceService.addPayment() based on cumulative payments.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InvoiceStatus status;

    /**
     * Payments applied against this invoice.
     * CascadeType.ALL means payments are saved and deleted with the invoice.
     */
    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Payment> payments;

    /** Possible states an Invoice can be in throughout its lifecycle. */
    public enum InvoiceStatus {
        /** Created but not yet sent to the customer. */
        DRAFT,
        /** Sent to the customer and awaiting payment. */
        SENT,
        /** Some payment has been received but the balance is still outstanding. */
        PARTIALLY_PAID,
        /** The full invoice amount has been received. */
        PAID,
        /** The invoice was cancelled (e.g. order cancelled, credit note issued). */
        CANCELLED
    }
}
