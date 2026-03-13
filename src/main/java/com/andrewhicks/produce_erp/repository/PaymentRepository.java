package com.andrewhicks.produce_erp.repository;

import com.andrewhicks.produce_erp.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.math.BigDecimal;
import java.util.List;

/**
 * Data access layer for Payment entities.
 *
 * Payments are applied against Invoices to reduce the outstanding balance.
 * The key query here is getTotalPaidByInvoice, which InvoiceService uses after
 * each new payment to decide whether to update the invoice status to
 * PARTIALLY_PAID or PAID.
 *
 * Used by: InvoiceService
 * Related: each Payment belongs to one Invoice (FK)
 */
@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    /**
     * Returns all payments applied to a given invoice, for payment history display.
     */
    List<Payment> findByInvoiceInvoiceId(Long invoiceId);

    /**
     * Sums all payment amounts for a given invoice.
     * Returns null (not 0) if no payments exist — callers must null-check.
     *
     * Used by InvoiceService.addPayment() to determine whether the invoice is
     * now PARTIALLY_PAID or fully PAID after each new payment is recorded.
     */
    @Query("SELECT SUM(p.amount) FROM Payment p WHERE p.invoice.invoiceId = :invoiceId")
    BigDecimal getTotalPaidByInvoice(Long invoiceId);
}
