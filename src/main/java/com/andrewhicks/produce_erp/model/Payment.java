package com.andrewhicks.produce_erp.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Represents a Payment — a single cash receipt applied against an Invoice.
 *
 * Payments are the final step in the sales lifecycle:
 *   SalesOrder → Invoice → Payment
 *
 * A single Invoice can receive multiple partial payments (e.g. a deposit followed
 * by a balance payment). After each payment is added via InvoiceService.addPayment(),
 * the service recalculates the total paid and updates the Invoice status accordingly:
 *   - Some paid → PARTIALLY_PAID
 *   - Fully paid → PAID
 *
 * Database table: payment
 */
@Entity
@Table(name = "payment")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Payment {

    /** Primary key, auto-incremented by the database. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "payment_id")
    private Long paymentId;

    /**
     * The invoice this payment is applied to.
     * Many payments can be applied to one invoice (partial payments).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id", nullable = false)
    private Invoice invoice;

    /** The date the payment was received. Defaults to today in InvoiceService.addPayment(). */
    @Column(name = "payment_date", nullable = false)
    private LocalDate paymentDate;

    /** The amount of this specific payment, in the system currency. */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    /**
     * How the payment was made (e.g. "BANK_TRANSFER", "CREDIT_CARD", "CHEQUE").
     * Free-text field — consider converting to an enum if you need strict validation.
     */
    @Column(name = "payment_method", nullable = false)
    private String paymentMethod;
}
