package com.andrewhicks.produce_erp.service;

import com.andrewhicks.produce_erp.exception.BusinessException;
import com.andrewhicks.produce_erp.exception.ResourceNotFoundException;
import com.andrewhicks.produce_erp.model.Invoice;
import com.andrewhicks.produce_erp.model.Payment;
import com.andrewhicks.produce_erp.model.SalesOrder;
import com.andrewhicks.produce_erp.repository.InvoiceRepository;
import com.andrewhicks.produce_erp.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Business logic layer for Invoice and Payment management.
 *
 * Invoices represent the financial obligation from a customer after goods are shipped.
 * This service handles the accounts receivable side of the sales lifecycle:
 *   SalesOrder (shipped) → Invoice → Payment(s) → Invoice marked PAID
 *
 * The key behaviour is in addPayment():
 *   - Each payment is recorded against the invoice
 *   - After saving, the total of all payments is summed via PaymentRepository
 *   - If total paid >= invoice total → status becomes PAID
 *   - If total paid > 0 but < total → status becomes PARTIALLY_PAID
 *   - This ensures invoice status always reflects the true payment position
 *
 * Multiple invoices can exist per SalesOrder (partial invoicing scenarios).
 * Multiple payments can exist per Invoice (instalment payments).
 *
 * Called by: InvoiceController
 */
@Service
@RequiredArgsConstructor
@Transactional
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final PaymentRepository paymentRepository;
    private final SalesOrderService salesOrderService;

    /** Returns all invoices. No filtering applied. */
    public List<Invoice> findAll() {
        return invoiceRepository.findAll();
    }

    /** Finds an invoice by ID or throws ResourceNotFoundException (HTTP 404). */
    public Invoice findById(Long id) {
        return invoiceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found with id: " + id));
    }

    /** Returns all invoices for a given sales order (for order-level AR view). */
    public List<Invoice> findBySalesOrder(Long salesOrderId) {
        return invoiceRepository.findBySalesOrderSalesOrderId(salesOrderId);
    }

    /** Filters invoices by status — for AR dashboards (e.g. "all unpaid invoices"). */
    public List<Invoice> findByStatus(Invoice.InvoiceStatus status) {
        return invoiceRepository.findByStatus(status);
    }

    /**
     * Creates a new invoice in DRAFT status for a given sales order.
     * Validates the sales order exists, sets the invoice date to today if not provided.
     * Does not auto-calculate amount — the caller provides the totalAmount.
     */
    public Invoice create(Invoice invoice) {
        SalesOrder so = salesOrderService.findById(invoice.getSalesOrder().getSalesOrderId());
        invoice.setSalesOrder(so);
        invoice.setStatus(Invoice.InvoiceStatus.DRAFT);
        if (invoice.getInvoiceDate() == null) invoice.setInvoiceDate(LocalDate.now());
        return invoiceRepository.save(invoice);
    }

    /**
     * Manually overrides the status of an invoice.
     * Used for transitions like DRAFT → SENT that don't require payment logic.
     */
    public Invoice updateStatus(Long id, Invoice.InvoiceStatus newStatus) {
        Invoice invoice = findById(id);
        invoice.setStatus(newStatus);
        return invoiceRepository.save(invoice);
    }

    /**
     * Records a payment against an invoice and automatically updates the invoice status.
     *
     * Steps:
     *   1. Guards against paying a PAID or CANCELLED invoice
     *   2. Links the payment to the invoice and defaults the payment date to today
     *   3. Saves the payment
     *   4. Recalculates total paid across all payments for this invoice
     *   5. Updates invoice status:
     *      - totalPaid >= totalAmount → PAID
     *      - totalPaid > 0           → PARTIALLY_PAID
     *
     * @throws BusinessException if the invoice is already PAID or CANCELLED
     */
    public Payment addPayment(Long invoiceId, Payment payment) {
        Invoice invoice = findById(invoiceId);
        if (invoice.getStatus() == Invoice.InvoiceStatus.PAID ||
                invoice.getStatus() == Invoice.InvoiceStatus.CANCELLED) {
            throw new BusinessException("Cannot add payment to a " + invoice.getStatus() + " invoice");
        }

        payment.setInvoice(invoice);
        if (payment.getPaymentDate() == null) payment.setPaymentDate(LocalDate.now());

        Payment saved = paymentRepository.save(payment);

        // Recalculate total paid and update invoice status accordingly
        BigDecimal totalPaid = paymentRepository.getTotalPaidByInvoice(invoiceId);
        if (totalPaid == null) totalPaid = BigDecimal.ZERO;

        if (totalPaid.compareTo(invoice.getTotalAmount()) >= 0) {
            invoice.setStatus(Invoice.InvoiceStatus.PAID);
        } else if (totalPaid.compareTo(BigDecimal.ZERO) > 0) {
            invoice.setStatus(Invoice.InvoiceStatus.PARTIALLY_PAID);
        }
        invoiceRepository.save(invoice);

        return saved;
    }

    /**
     * Returns all payments recorded against a given invoice.
     * Used for the payment history view on an invoice.
     */
    public List<Payment> findPaymentsByInvoice(Long invoiceId) {
        findById(invoiceId); // validate invoice exists first
        return paymentRepository.findByInvoiceInvoiceId(invoiceId);
    }
}
