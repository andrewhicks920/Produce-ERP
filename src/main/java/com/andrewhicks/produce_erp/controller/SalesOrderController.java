package com.andrewhicks.produce_erp.controller;

import com.andrewhicks.produce_erp.model.SalesOrder;
import com.andrewhicks.produce_erp.service.SalesOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST controller for Sales Order management.
 *
 * Sales Orders drive the outbound inventory flow. The most significant endpoint
 * here is POST /ship, which triggers FIFO lot consumption and stock deduction.
 *
 * Base URL: /api/sales-orders
 *
 * Endpoints:
 *   GET    /api/sales-orders                → list all (filter by ?customerId= or ?status=)
 *   GET    /api/sales-orders/{id}           → get one order
 *   POST   /api/sales-orders                → create a new order
 *   PUT    /api/sales-orders/{id}           → update a DRAFT order
 *   PATCH  /api/sales-orders/{id}/status   → change order status (e.g. DRAFT → CONFIRMED)
 *   POST   /api/sales-orders/{id}/ship     → ship the order (FIFO stock deduction)
 *   DELETE /api/sales-orders/{id}           → delete a DRAFT order
 *
 * The ship endpoint triggers:
 *   FIFO lot selection → InventoryTransactions (SALE, negative) → Lot.remainingQuantity updates
 *
 * All business logic is delegated to SalesOrderService.
 */
@RestController
@RequestMapping("/api/sales-orders")
@RequiredArgsConstructor
public class SalesOrderController {

    private final SalesOrderService salesOrderService;

    /**
     * Returns all sales orders. Supports optional filtering:
     *   ?customerId=1    → all orders for a given customer
     *   ?status=SHIPPED  → all orders with a specific status
     */
    @GetMapping
    public ResponseEntity<List<SalesOrder>> findAll(
            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) SalesOrder.SalesOrderStatus status) {
        if (customerId != null) return ResponseEntity.ok(salesOrderService.findByCustomer(customerId));
        if (status != null) return ResponseEntity.ok(salesOrderService.findByStatus(status));
        return ResponseEntity.ok(salesOrderService.findAll());
    }

    /** Returns a single sales order by ID. Returns 404 if not found. */
    @GetMapping("/{id}")
    public ResponseEntity<SalesOrder> findById(@PathVariable Long id) {
        return ResponseEntity.ok(salesOrderService.findById(id));
    }

    /**
     * Creates a new sales order in DRAFT status.
     * Request body should include a customer reference and one or more order lines.
     * Total amount is calculated automatically from the lines.
     */
    @PostMapping
    public ResponseEntity<SalesOrder> create(@RequestBody SalesOrder salesOrder) {
        return ResponseEntity.status(HttpStatus.CREATED).body(salesOrderService.create(salesOrder));
    }

    /**
     * Updates editable fields of a DRAFT sales order.
     * Returns 400 if the order has progressed beyond DRAFT.
     */
    @PutMapping("/{id}")
    public ResponseEntity<SalesOrder> update(@PathVariable Long id, @RequestBody SalesOrder salesOrder) {
        return ResponseEntity.ok(salesOrderService.update(id, salesOrder));
    }

    /**
     * Updates the status of a sales order.
     * Request body: { "status": "CONFIRMED" }
     * Typical use: DRAFT → CONFIRMED to mark an order ready for shipment.
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<SalesOrder> updateStatus(@PathVariable Long id,
            @RequestBody Map<String, String> body) {
        SalesOrder.SalesOrderStatus status = SalesOrder.SalesOrderStatus.valueOf(body.get("status"));
        return ResponseEntity.ok(salesOrderService.updateStatus(id, status));
    }

    /**
     * Ships a CONFIRMED sales order.
     * Triggers FIFO lot consumption: walks each line's available lots (oldest first),
     * deducts quantities, and posts SALE InventoryTransactions.
     * Returns 400 if the order is not CONFIRMED or if any product has insufficient stock.
     */
    @PostMapping("/{id}/ship")
    public ResponseEntity<SalesOrder> ship(@PathVariable Long id) {
        return ResponseEntity.ok(salesOrderService.ship(id));
    }

    /**
     * Deletes a DRAFT sales order.
     * Returns 400 if the order has been confirmed or shipped.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        salesOrderService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
