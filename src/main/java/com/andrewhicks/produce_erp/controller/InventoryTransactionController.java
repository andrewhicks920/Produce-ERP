package com.andrewhicks.produce_erp.controller;

import com.andrewhicks.produce_erp.model.InventoryTransaction;
import com.andrewhicks.produce_erp.service.InventoryTransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * REST controller for Inventory Transaction queries and manual posting.
 *
 * InventoryTransactions are the immutable audit log of all stock movements. Most
 * transactions are created automatically by PurchaseOrderService (receipts),
 * SalesOrderService (sales), and InventoryAdjustmentService (adjustments).
 *
 * This controller primarily exists for read access — querying transaction history —
 * but also exposes a POST endpoint for advanced use cases where a transaction needs
 * to be posted directly (e.g. integrations, data migrations, RETURN transactions).
 *
 * Base URL: /api/inventory-transactions
 *
 * Endpoints:
 *   GET  /api/inventory-transactions                         → list all (various filters)
 *   GET  /api/inventory-transactions/{id}                   → get one transaction
 *   POST /api/inventory-transactions                        → post a transaction manually
 *
 * Filtering via query params (mutually exclusive, first match wins):
 *   ?productId=1                         → all transactions for a product
 *   ?lotId=5                             → all transactions for a specific lot
 *   ?from=2024-01-01T00:00&to=2024-12-31T23:59 → transactions within a date range
 *
 * All business logic is delegated to InventoryTransactionService.
 */
@RestController
@RequestMapping("/api/inventory-transactions")
@RequiredArgsConstructor
public class InventoryTransactionController {

    private final InventoryTransactionService transactionService;

    /**
     * Returns inventory transactions with optional filtering.
     * If no filter params are provided, returns all transactions (consider pagination for large datasets).
     *
     * Filter priority: productId > lotId > date range > all
     */
    @GetMapping
    public ResponseEntity<List<InventoryTransaction>> findAll(
            @RequestParam(required = false) Long productId,
            @RequestParam(required = false) Long lotId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {

        if (productId != null) return ResponseEntity.ok(transactionService.findByProduct(productId));
        if (lotId != null) return ResponseEntity.ok(transactionService.findByLot(lotId));
        if (from != null && to != null) return ResponseEntity.ok(transactionService.findByDateRange(from, to));
        return ResponseEntity.ok(transactionService.findAll());
    }

    /** Returns a single transaction by ID. Returns 404 if not found. */
    @GetMapping("/{id}")
    public ResponseEntity<InventoryTransaction> findById(@PathVariable Long id) {
        return ResponseEntity.ok(transactionService.findById(id));
    }

    /**
     * Manually posts an inventory transaction (advanced use — prefer domain actions like ship/receive).
     * Requires product, lot, transactionType, and quantityChange in the request body.
     * Also updates the lot's remainingQuantity via LotService.adjustRemainingQuantity().
     * Returns the saved transaction with HTTP 201.
     */
    @PostMapping
    public ResponseEntity<InventoryTransaction> record(@RequestBody InventoryTransaction transaction) {
        return ResponseEntity.status(HttpStatus.CREATED).body(transactionService.record(transaction));
    }
}
