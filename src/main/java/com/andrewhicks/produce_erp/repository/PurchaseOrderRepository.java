package com.andrewhicks.produce_erp.repository;

import com.andrewhicks.produce_erp.model.PurchaseOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, Long> {
    // Returns all POs for a given supplier, useful for the supplier order history view.
    // Note: uses the nested path supplier.supplierId to traverse the FK relationship.
    List<PurchaseOrder> findBySupplierId(Long supplierId);

    // Filters POs by their lifecycle status (DRAFT, SENT, RECEIVED, etc.).
    // Used for dashboards (e.g. "show all open orders") and workflow filtering.
    List<PurchaseOrder> findByStatus(PurchaseOrder.PurchaseOrderStatus status);
}
