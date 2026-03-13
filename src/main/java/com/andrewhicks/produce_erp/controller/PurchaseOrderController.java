package com.andrewhicks.produce_erp.controller;

import com.andrewhicks.produce_erp.model.Lot;
import com.andrewhicks.produce_erp.model.PurchaseOrder;
import com.andrewhicks.produce_erp.service.PurchaseOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * REST controller for Purchase Order management.
 *
 * Purchase Orders drive the inbound inventory flow. This controller covers the full
 * PO lifecycle from creation through to goods receipt, which physically adds stock.
 *
 * Base URL: /api/purchase-orders
 *
 * Endpoints:
 *   GET    /api/purchase-orders                              → list all (filter by ?supplierId= or ?status=)
 *   GET    /api/purchase-orders/{id}                        → get one PO
 *   POST   /api/purchase-orders                             → create a new PO
 *   PUT    /api/purchase-orders/{id}                        → update a DRAFT PO
 *   PATCH  /api/purchase-orders/{id}/status                 → change PO status (e.g. DRAFT → SENT)
 *   POST   /api/purchase-orders/{poId}/lines/{lineId}/receive → receive goods against a PO line
 *   DELETE /api/purchase-orders/{id}                        → delete a DRAFT PO
 *
 * The receive endpoint is the most important — it triggers:
 *   Lot creation + InventoryTransaction (RECEIPT) + PO status update
 *
 * All business logic is delegated to PurchaseOrderService.
 */
@RestController
@RequestMapping("/api/purchase-orders")
@RequiredArgsConstructor
public class PurchaseOrderController {

    private final PurchaseOrderService purchaseOrderService;

    /**
     * Returns all purchase orders. Supports optional filtering:
     *   ?supplierId=1    → all POs for a given supplier
     *   ?status=SENT     → all POs with a specific status
     */
    @GetMapping
    public ResponseEntity<List<PurchaseOrder>> findAll(
            @RequestParam(required = false) Long supplierId,
            @RequestParam(required = false) PurchaseOrder.PurchaseOrderStatus status) {
        if (supplierId != null) return ResponseEntity.ok(purchaseOrderService.findBySupplier(supplierId));
        if (status != null) return ResponseEntity.ok(purchaseOrderService.findByStatus(status));
        return ResponseEntity.ok(purchaseOrderService.findAll());
    }

    /** Returns a single purchase order by ID. Returns 404 if not found. */
    @GetMapping("/{id}")
    public ResponseEntity<PurchaseOrder> findById(@PathVariable Long id) {
        return ResponseEntity.ok(purchaseOrderService.findById(id));
    }

    /**
     * Creates a new purchase order in DRAFT status.
     * Request body should include a supplier reference and one or more PO lines.
     */
    @PostMapping
    public ResponseEntity<PurchaseOrder> create(@RequestBody PurchaseOrder po) {
        return ResponseEntity.status(HttpStatus.CREATED).body(purchaseOrderService.create(po));
    }

    /**
     * Updates editable fields of a DRAFT purchase order.
     * Returns 400 if the PO is not in DRAFT status.
     */
    @PutMapping("/{id}")
    public ResponseEntity<PurchaseOrder> update(@PathVariable Long id, @RequestBody PurchaseOrder po) {
        return ResponseEntity.ok(purchaseOrderService.update(id, po));
    }

    /**
     * Updates the status of a purchase order.
     * Request body: { "status": "SENT" }
     * Used for manual transitions like DRAFT → SENT (sending to supplier).
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<PurchaseOrder> updateStatus(@PathVariable Long id,
            @RequestBody Map<String, String> body) {
        PurchaseOrder.PurchaseOrderStatus status =
                PurchaseOrder.PurchaseOrderStatus.valueOf(body.get("status"));
        return ResponseEntity.ok(purchaseOrderService.updateStatus(id, status));
    }

    /**
     * Records the receipt of goods against a specific PO line.
     * This is the inbound inventory trigger — it creates a Lot and posts an
     * InventoryTransaction of type RECEIPT.
     *
     * Request body:
     * {
     *   "quantity": 50,
     *   "lotNumber": "LOT-2024-001",
     *   "expirationDate": "2026-12-31"   // optional
     * }
     *
     * Returns the newly created Lot.
     * Returns 400 if quantity exceeds outstanding amount.
     */
    @PostMapping("/{poId}/lines/{poLineId}/receive")
    public ResponseEntity<Lot> receivePoLine(
            @PathVariable Long poId,
            @PathVariable Long poLineId,
            @RequestBody Map<String, Object> body) {

        int qty = (int) body.get("quantity");
        String lotNumber = (String) body.get("lotNumber");
        LocalDate expDate = body.get("expirationDate") != null
                ? LocalDate.parse((String) body.get("expirationDate")) : null;

        return ResponseEntity.ok(purchaseOrderService.receivePoLine(poId, poLineId, qty, lotNumber, expDate));
    }

    /**
     * Deletes a DRAFT purchase order.
     * Returns 400 if the PO has been sent or has received any stock.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        purchaseOrderService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
