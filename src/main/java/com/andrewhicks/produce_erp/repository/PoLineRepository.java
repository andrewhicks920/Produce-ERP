package com.andrewhicks.produce_erp.repository;

import com.andrewhicks.produce_erp.model.PoLine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

/**
 * Data access layer for PoLine (Purchase Order Line) entities.
 *
 * PoLines are children of PurchaseOrder. They are the per-product breakdown
 * of what is being ordered and at what cost.
 *
 * Used by: PurchaseOrderService (indirectly, via PurchaseOrder.getPoLines())
 */
@Repository
public interface PoLineRepository extends JpaRepository<PoLine, Long> {

    /**
     * Retrieves all lines for a given purchase order.
     * Useful when you need the lines without loading the whole PurchaseOrder graph.
     */
    List<PoLine> findByPurchaseOrderPoId(Long poId);

    /**
     * Retrieves all PO lines that reference a particular product.
     * Useful for purchase history per product (e.g. "what have we ordered of SKU X?").
     */
    List<PoLine> findByProductProductId(Long productId);
}
