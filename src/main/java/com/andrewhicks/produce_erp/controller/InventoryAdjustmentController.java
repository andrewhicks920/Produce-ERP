package com.andrewhicks.produce_erp.controller;

import com.andrewhicks.produce_erp.model.InventoryAdjustment;
import com.andrewhicks.produce_erp.service.InventoryAdjustmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for Inventory Adjustment management.
 *
 * Inventory adjustments are operator-initiated corrections to stock levels,
 * targeting specific lots directly (unlike sales which use FIFO automatically).
 *
 * Common scenarios:
 *   - Annual stocktake reveals a discrepancy → post adjustment to correct it
 *   - Damaged goods found in warehouse → write off units from affected lot
 *   - Uncounted stock discovered → add units back to a lot
 *
 * Creating an adjustment (POST) automatically:
 *   1. Saves the adjustment header + all lines
 *   2. Posts an InventoryTransaction (ADJUSTMENT type) per line
 *   3. Updates each affected Lot's remainingQuantity
 *
 * Base URL: /api/inventory-adjustments
 *
 * Endpoints:
 *   GET  /api/inventory-adjustments      → list all adjustments
 *   GET  /api/inventory-adjustments/{id} → get one adjustment
 *   POST /api/inventory-adjustments      → create and apply a new adjustment
 *
 * All business logic is delegated to InventoryAdjustmentService.
 */
@RestController
@RequestMapping("/api/inventory-adjustments")
@RequiredArgsConstructor
public class InventoryAdjustmentController {

    private final InventoryAdjustmentService adjustmentService;

    /** Returns all inventory adjustments (full list — add filters/pagination as needed). */
    @GetMapping
    public ResponseEntity<List<InventoryAdjustment>> findAll() {
        return ResponseEntity.ok(adjustmentService.findAll());
    }

    /** Returns a single adjustment by ID including all its lines. Returns 404 if not found. */
    @GetMapping("/{id}")
    public ResponseEntity<InventoryAdjustment> findById(@PathVariable Long id) {
        return ResponseEntity.ok(adjustmentService.findById(id));
    }

    /**
     * Creates and immediately applies an inventory adjustment.
     *
     * Request body example:
     * {
     *   "reason": "Q4 Stocktake correction",
     *   "createdBy": "jane.smith",
     *   "adjustmentLines": [
     *     { "product": { "productId": 1 }, "lot": { "lotId": 3 }, "quantityChange": -5 },
     *     { "product": { "productId": 2 }, "lot": { "lotId": 7 }, "quantityChange": 10 }
     *   ]
     * }
     *
     * Returns the created adjustment with HTTP 201.
     * The stock changes are applied immediately and are non-reversible.
     */
    @PostMapping
    public ResponseEntity<InventoryAdjustment> create(@RequestBody InventoryAdjustment adjustment) {
        return ResponseEntity.status(HttpStatus.CREATED).body(adjustmentService.create(adjustment));
    }
}
