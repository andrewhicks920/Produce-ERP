package com.andrewhicks.produce_erp.repository;

import com.andrewhicks.produce_erp.model.SalesOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

/**
 * Data access layer for SalesOrder entities.
 *
 * SalesOrders are the core of the outbound fulfilment flow. This repository
 * supports filtering by customer (for order history) and by status (for workflow views).
 *
 * Used by: SalesOrderService
 * Related: each SalesOrder has a Customer (FK), SalesOrderLines (children), and Invoices (children)
 */
@Repository
public interface SalesOrderRepository extends JpaRepository<SalesOrder, Long> {

    /**
     * Returns all orders placed by a specific customer, for order history views.
     * Traverses the customer FK: WHERE customer.customer_id = :customerId
     */
    List<SalesOrder> findByCustomerCustomerId(Long customerId);

    /**
     * Filters orders by lifecycle status (DRAFT, CONFIRMED, SHIPPED, etc.).
     * Used for workflow dashboards (e.g. "orders ready to ship").
     */
    List<SalesOrder> findByStatus(SalesOrder.SalesOrderStatus status);
}
