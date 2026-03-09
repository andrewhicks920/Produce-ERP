package com.andrewhicks.produce_erp.controller;

import com.andrewhicks.produce_erp.model.PurchaseOrder;
import com.andrewhicks.produce_erp.service.PurchaseOrderService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST controller for Purchase Order management.
 *
 * Purchase Orders drive the inbound inventory flow. This controller covers the full
 * PO lifecycle from creation through to goods receipt, which physically adds stock.
 *
 * Base URL: /purchase-orders
 *
 * Endpoints:
 *   GET    /purchase-orders                              → list all (filter by ?supplierId= or ?status=)
 *   GET    /purchase-orders/{id}                        → get one PO
 *   POST   /purchase-orders                             → create a new PO
 *   PUT    /purchase-orders/{id}                        → update a DRAFT PO
 *   PATCH  /urchase-orders/{id}/status                 → change PO status (e.g. DRAFT → SENT)
 *   POST   /purchase-orders/{poId}/lines/{lineId}/receive → receive goods against a PO line
 *   DELETE /purchase-orders/{id}                        → delete a DRAFT PO
 *
 * The receive endpoint triggers:
 *   Lot creation + InventoryTransaction (RECEIPT) + PO status update
 */
@RestController
@RequestMapping("/purchase-orders")
public class PurchaseOrderController {
    private final PurchaseOrderService purchaseOrderService;

    public PurchaseOrderController(PurchaseOrderService purchaseOrderService) {
        this.purchaseOrderService = purchaseOrderService;
    }

    /**
     * Returns all purchase orders. Supports optional filtering:
     *   ?supplierId=1    → all POs for a given supplier
     *   ?status=SENT     → all POs with a specific status
     */
    @GetMapping
    public List<PurchaseOrder> getPurchaseOrders(@RequestParam(required = false) Long supplierId, @RequestParam(required = false) PurchaseOrder.PurchaseOrderStatus status) {
        if (supplierId != null)
            return purchaseOrderService.getPurchaseOrderBySupplier(supplierId);
        if (status != null)
            return purchaseOrderService.getPurchaseOrderByStatus(status);

        return purchaseOrderService.getAllPurchaseOrders();
    }


    /** Returns a single purchase order by ID. Returns 404 if not found. */
    @GetMapping("/{id}")
    public PurchaseOrder findById(@PathVariable Long id) {
        return purchaseOrderService.getPurchaseOrderByID(id);
    }

    /**
     * Creates a new purchase order in DRAFT status.
     * Request body should include a supplier reference and one or more PO lines.
     */
    @PostMapping
    public PurchaseOrder create(@RequestBody PurchaseOrder po) {
        return purchaseOrderService.createPurchaseOrder(po);
    }

    /**
     * Updates editable fields of a DRAFT purchase order.
     * Returns RTE if the PO is not in DRAFT status.
     */
    @PutMapping("/{id}")
    public PurchaseOrder updatePurchaseOrderByID(@PathVariable Long id, @RequestBody PurchaseOrder po) {
        return purchaseOrderService.update(id, po);
    }

    /**
     * Updates the status of a purchase order.
     * Request body: { "status": "SENT" }
     * Used for manual transitions like DRAFT → SENT (sending to supplier).
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<PurchaseOrder> updatePurchaseOrderByStatus(@PathVariable Long id, @RequestBody Map<String, String> body) {
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
//    @PostMapping("/{poId}/lines/{poLineId}/receive")
//    public ResponseEntity<Lot> receivePoLine(
//            @PathVariable Long poId,
//            @PathVariable Long poLineId,
//            @RequestBody Map<String, Object> body) {
//
//        int qty = (int) body.get("quantity");
//        String lotNumber = (String) body.get("lotNumber");
//        LocalDate expDate = body.get("expirationDate") != null
//                ? LocalDate.parse((String) body.get("expirationDate")) : null;
//
//        return ResponseEntity.ok(purchaseOrderService.receivePoLine(poId, poLineId, qty, lotNumber, expDate));
//    }

    //Deletes a DRAFT purchase order.
    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        purchaseOrderService.delete(id);
    }
}
