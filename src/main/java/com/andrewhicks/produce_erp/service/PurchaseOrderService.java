package com.andrewhicks.produce_erp.service;

import com.andrewhicks.produce_erp.model.PurchaseOrder;
import com.andrewhicks.produce_erp.model.Supplier;
import com.andrewhicks.produce_erp.repository.PurchaseOrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PurchaseOrderService {
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final SupplierService supplierService;
    private final ProductService productService;
    private final InventoryTransactionService transactionService;

    public List<PurchaseOrder> getAllPurchaseOrders() {
        return purchaseOrderRepository.findAll();
    }

    public PurchaseOrder getPurchaseOrderByID(Long id) {
        return purchaseOrderRepository.findById(id).orElseThrow(()
                -> new RuntimeException("Purchase order not found with id: " + id));
    }

    public List<PurchaseOrder> getPurchaseOrderBySupplier(Long supplierId) {
        return purchaseOrderRepository.findBySupplierId(supplierId);
    }

    public List<PurchaseOrder> getPurchaseOrderByStatus(PurchaseOrder.PurchaseOrderStatus status) {
        return purchaseOrderRepository.findByStatus(status);
    }

    /**
     * Creates a new Purchase Order in DRAFT status.
     * Validates the supplier exists and defaults the order date to today.
     * Lines (PoLine) are persisted via CascadeType.ALL on the entity.
     */
    public PurchaseOrder createPurchaseOrder(PurchaseOrder purchaseOrder) {
        Supplier supplier = supplierService.getSupplierById(purchaseOrder.getSupplier().getId());
        purchaseOrder.setSupplier(supplier);

        purchaseOrder.setStatus(PurchaseOrder.PurchaseOrderStatus.DRAFT);
        if (purchaseOrder.getOrderDate() == null)
            purchaseOrder.setOrderDate(LocalDate.now());

        return purchaseOrderRepository.save(purchaseOrder);
    }

    /**
     * Updates editable fields of a DRAFT Purchase Order.
     * Throws RTE if the PO is no longer in DRAFT status, since changes to sent
     * or received orders would cause data integrity issues.
     */
    public PurchaseOrder update(Long id, PurchaseOrder updated) {
        PurchaseOrder existing = getPurchaseOrderByID(id);

        if (existing.getStatus() != PurchaseOrder.PurchaseOrderStatus.DRAFT)
            throw new RuntimeException("Only DRAFT purchase orders can be updated");

        existing.setExpectedDate(updated.getExpectedDate());
        if (updated.getSupplier() != null)
            existing.setSupplier(supplierService.getSupplierById(updated.getSupplier().getId()));

        return purchaseOrderRepository.save(existing);
    }

    /**
     * Manually overrides the status of a PurchaseOrder.
     * Useful for transitions like DRAFT → SENT that don't require business logic.
     * Status transitions with side effects (like receiving) use dedicated methods.
     */
    public PurchaseOrder updateStatus(Long id, PurchaseOrder.PurchaseOrderStatus newStatus) {
        PurchaseOrder po = getPurchaseOrderByID(id);
        po.setStatus(newStatus);

        return purchaseOrderRepository.save(po);
    }

    /**
     * Deletes a DRAFT purchase order.
     * Throws BusinessException if the PO is not in DRAFT status — you cannot
     * delete a PO that has been sent to a supplier or has received stock.
     */
    public void delete(Long id) {
        PurchaseOrder po = getPurchaseOrderByID(id);
        if (po.getStatus() != PurchaseOrder.PurchaseOrderStatus.DRAFT)
            throw new RuntimeException("Only DRAFT purchase orders can be deleted");

        purchaseOrderRepository.deleteById(id);
    }
}
