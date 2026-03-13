package com.andrewhicks.produce_erp.repository;

import com.andrewhicks.produce_erp.model.SalesOrderLine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

/**
 * Data access layer for SalesOrderLine entities.
 *
 * SalesOrderLines are the per-product breakdown of a SalesOrder.
 * They drive FIFO lot selection during shipment in SalesOrderService.ship().
 *
 * Used by: SalesOrderService (indirectly, via SalesOrder.getSalesOrderLines())
 */
@Repository
public interface SalesOrderLineRepository extends JpaRepository<SalesOrderLine, Long> {

    /**
     * Returns all lines for a given sales order.
     * Useful when you need lines without loading the full SalesOrder graph.
     */
    List<SalesOrderLine> findBySalesOrderSalesOrderId(Long salesOrderId);

    /**
     * Returns all order lines that reference a specific product.
     * Useful for sales history per product (e.g. "what have we sold of SKU X?").
     */
    List<SalesOrderLine> findByProductProductId(Long productId);
}
