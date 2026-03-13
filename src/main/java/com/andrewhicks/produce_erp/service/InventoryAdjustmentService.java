package com.andrewhicks.produce_erp.service;

import com.andrewhicks.produce_erp.exception.ResourceNotFoundException;
import com.andrewhicks.produce_erp.model.*;
import com.andrewhicks.produce_erp.repository.InventoryAdjustmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Business logic layer for Inventory Adjustment management.
 *
 * Inventory adjustments are manual, operator-driven corrections to stock levels.
 * Unlike receipts (which come from POs) and sales (which come from sales orders),
 * adjustments are ad-hoc and target specific lots directly.
 *
 * Typical use cases:
 *   - Stocktake discrepancies (system shows 100, actual count is 95 → adjust -5)
 *   - Damaged goods write-off (lot partially destroyed → adjust negative)
 *   - Found stock correction (uncounted units discovered → adjust positive)
 *
 * When create() is called, it:
 *   1. Sets the createdDate timestamp automatically
 *   2. Validates and resolves product and lot references for each AdjustmentLine
 *   3. Saves the adjustment header and all lines (via CascadeType.ALL)
 *   4. Posts an InventoryTransaction (type ADJUSTMENT) for each line,
 *      which also updates the affected Lot's remainingQuantity
 *
 * Adjustments explicitly target a specific lot (unlike sales which use FIFO),
 * because the operator knows exactly which batch needs correcting.
 *
 * Called by: InventoryAdjustmentController
 */
@Service
@RequiredArgsConstructor
@Transactional
public class InventoryAdjustmentService {

    private final InventoryAdjustmentRepository adjustmentRepository;
    private final ProductService productService;
    private final LotService lotService;
    private final InventoryTransactionService transactionService;

    /** Returns all inventory adjustments. No filtering applied. */
    public List<InventoryAdjustment> findAll() {
        return adjustmentRepository.findAll();
    }

    /** Finds an adjustment by ID or throws ResourceNotFoundException (HTTP 404). */
    public InventoryAdjustment findById(Long id) {
        return adjustmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Adjustment not found with id: " + id));
    }

    /**
     * Creates a new inventory adjustment and posts InventoryTransactions for each line.
     *
     * Steps:
     *   1. Stamps createdDate as now (set by the service, not the caller)
     *   2. Validates each AdjustmentLine's product and lot FKs
     *   3. Sets the back-reference from each line to its parent adjustment
     *   4. Saves the adjustment (lines cascade-save with it)
     *   5. For each line, posts an ADJUSTMENT InventoryTransaction, which also
     *      adjusts Lot.remainingQuantity via LotService.adjustRemainingQuantity()
     *
     * The entire operation is @Transactional — if any step fails, all changes roll back.
     */
    public InventoryAdjustment create(InventoryAdjustment adjustment) {
        adjustment.setCreatedDate(LocalDateTime.now());

        // Validate and link all line references before saving
        if (adjustment.getAdjustmentLines() != null) {
            for (AdjustmentLine line : adjustment.getAdjustmentLines()) {
                Product product = productService.findById(line.getProduct().getProductId());
                Lot lot = lotService.findById(line.getLot().getLotId());
                line.setProduct(product);
                line.setLot(lot);
                line.setInventoryAdjustment(adjustment); // set bi-directional back-reference
            }
        }

        InventoryAdjustment saved = adjustmentRepository.save(adjustment);

        // Post an InventoryTransaction for each adjustment line
        if (saved.getAdjustmentLines() != null) {
            for (AdjustmentLine line : saved.getAdjustmentLines()) {
                InventoryTransaction txn = InventoryTransaction.builder()
                        .product(line.getProduct())
                        .lot(line.getLot())
                        .transactionType(InventoryTransaction.TransactionType.ADJUSTMENT)
                        .quantityChange(line.getQuantityChange()) // can be + or -
                        .unitCost(line.getLot().getUnitCost())    // use existing lot cost
                        .referenceType(InventoryTransaction.ReferenceType.ADJUSTMENT)
                        .referenceId(saved.getAdjustmentId())
                        .transactionDate(LocalDateTime.now())
                        .build();
                transactionService.record(txn); // also updates lot.remainingQuantity
            }
        }

        return saved;
    }
}
