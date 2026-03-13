package com.andrewhicks.produce_erp.repository;

import com.andrewhicks.produce_erp.model.AdjustmentLine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

/**
 * Data access layer for AdjustmentLine entities.
 *
 * AdjustmentLines are children of InventoryAdjustment and represent individual
 * lot-level stock corrections. They are typically accessed via the parent
 * InventoryAdjustment's cascaded relationship, but these queries enable
 * product-centric and adjustment-centric lookups.
 *
 * Used by: InventoryAdjustmentService (indirectly, via InventoryAdjustment.getAdjustmentLines())
 */
@Repository
public interface AdjustmentLineRepository extends JpaRepository<AdjustmentLine, Long> {

    /**
     * Returns all adjustment lines within a specific adjustment.
     * Useful when viewing the detail of an adjustment without loading the parent object.
     */
    List<AdjustmentLine> findByInventoryAdjustmentAdjustmentId(Long adjustmentId);

    /**
     * Returns all adjustment lines that touched a specific product.
     * Useful for product-level adjustment history (e.g. "all manual corrections to SKU X").
     */
    List<AdjustmentLine> findByProductProductId(Long productId);
}
