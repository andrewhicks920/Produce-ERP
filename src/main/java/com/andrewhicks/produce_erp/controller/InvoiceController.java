package com.andrewhicks.produce_erp.controller;

import com.andrewhicks.produce_erp.model.Invoice;
import com.andrewhicks.produce_erp.model.Payment;
import com.andrewhicks.produce_erp.service.InvoiceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST controller for Invoice and Payment management.
 *
 * Invoices represent the financial obligation created when goods are shipped to a customer.
 * Payments reduce the outstanding balance on an invoice and automatically update its status.
 *
 * This controller covers the accounts receivable workflow:
 *   1. Create invoice → linked to a shipped SalesOrder
 *   2. Update status → send invoice to customer (DRAFT → SENT)
 *   3. Add payments → record received cash; invoice auto-updates to PARTIALLY_PAID or PAID
 *
 * Base URL: /api/invoices
 *
 * Endpoints:
 *   GET    /api/invoices                    → list all (filter by ?salesOrderId= or ?status=)
 *   GET    /api/invoices/{id}               → get one invoice
 *   POST   /api/invoices                    → create a new invoice
 *   PATCH  /api/invoices/{id}/status        → change invoice status (e.g. DRAFT → SENT)
 *   GET    /api/invoices/{id}/payments      → list all payments for an invoice
 *   POST   /api/invoices/{id}/payments      → add a payment to an invoice
 *
 * All business logic is delegated to InvoiceService.
 */
@RestController
@RequestMapping("/api/invoices")
@RequiredArgsConstructor
public class InvoiceController {

    private final InvoiceService invoiceService;

    /**
     * Returns all invoices. Supports optional filtering:
     *   ?salesOrderId=1  → invoices for a specific sales order
     *   ?status=SENT     → invoices with a specific payment status
     */
    @GetMapping
    public ResponseEntity<List<Invoice>> findAll(
            @RequestParam(required = false) Long salesOrderId,
            @RequestParam(required = false) Invoice.InvoiceStatus status) {
        if (salesOrderId != null) return ResponseEntity.ok(invoiceService.findBySalesOrder(salesOrderId));
        if (status != null) return ResponseEntity.ok(invoiceService.findByStatus(status));
        return ResponseEntity.ok(invoiceService.findAll());
    }

    /** Returns a single invoice by ID. Returns 404 if not found. */
    @GetMapping("/{id}")
    public ResponseEntity<Invoice> findById(@PathVariable Long id) {
        return ResponseEntity.ok(invoiceService.findById(id));
    }

    /**
     * Creates a new invoice linked to a SalesOrder.
     * Request body must include a salesOrder reference and totalAmount.
     * Status defaults to DRAFT.
     */
    @PostMapping
    public ResponseEntity<Invoice> create(@RequestBody Invoice invoice) {
        return ResponseEntity.status(HttpStatus.CREATED).body(invoiceService.create(invoice));
    }

    /**
     * Updates the status of an invoice.
     * Request body: { "status": "SENT" }
     * Used for manual transitions like DRAFT → SENT.
     * Payment-driven status changes (PARTIALLY_PAID, PAID) happen automatically via addPayment().
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<Invoice> updateStatus(@PathVariable Long id,
            @RequestBody Map<String, String> body) {
        Invoice.InvoiceStatus status = Invoice.InvoiceStatus.valueOf(body.get("status"));
        return ResponseEntity.ok(invoiceService.updateStatus(id, status));
    }

    /**
     * Returns all payments recorded against a given invoice, in creation order.
     * Used for the payment history view on an invoice detail page.
     */
    @GetMapping("/{id}/payments")
    public ResponseEntity<List<Payment>> getPayments(@PathVariable Long id) {
        return ResponseEntity.ok(invoiceService.findPaymentsByInvoice(id));
    }

    /**
     * Records a payment against an invoice and updates the invoice status.
     *
     * Request body example:
     * {
     *   "amount": 500.00,
     *   "paymentMethod": "BANK_TRANSFER",
     *   "paymentDate": "2024-03-15"   // optional, defaults to today
     * }
     *
     * After saving, the service recalculates total paid and sets invoice status
     * to PARTIALLY_PAID or PAID automatically.
     * Returns 400 if the invoice is already PAID or CANCELLED.
     */
    @PostMapping("/{id}/payments")
    public ResponseEntity<Payment> addPayment(@PathVariable Long id, @RequestBody Payment payment) {
        return ResponseEntity.status(HttpStatus.CREATED).body(invoiceService.addPayment(id, payment));
    }
}
