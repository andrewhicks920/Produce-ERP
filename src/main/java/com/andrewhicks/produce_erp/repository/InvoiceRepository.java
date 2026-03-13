package com.andrewhicks.produce_erp.repository;

import com.andrewhicks.produce_erp.model.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

/**
 * Data access layer for Invoice entities.
 *
 * Invoices are the financial documents sent to customers after shipment.
 * This repository supports looking up invoices by their parent sales order
 * and by payment status.
 *
 * Used by: InvoiceService
 * Related: each Invoice has a SalesOrder (FK) and many Payments (children)
 */
@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    /**
     * Returns all invoices raised against a specific sales order.
     * A single order may have multiple invoices in partial-invoicing scenarios.
     */
    List<Invoice> findBySalesOrderSalesOrderId(Long salesOrderId);

    /**
     * Filters invoices by payment status (DRAFT, SENT, PARTIALLY_PAID, PAID, CANCELLED).
     * Used for accounts receivable dashboards (e.g. "show all overdue unpaid invoices").
     */
    List<Invoice> findByStatus(Invoice.InvoiceStatus status);
}
