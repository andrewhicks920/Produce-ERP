package com.andrewhicks.produce_erp.service;

import com.andrewhicks.produce_erp.exception.BusinessException;
import com.andrewhicks.produce_erp.exception.ResourceNotFoundException;
import com.andrewhicks.produce_erp.model.*;
import com.andrewhicks.produce_erp.repository.PurchaseOrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Business logic layer for Purchase Order management.
 *
 * Purchase Orders drive the inbound stock flow. This is one of the most important
 * services in the system because it is responsible for physically adding stock via
 * the receivePoLine() method.
 *
 * Full inbound flow:
 *   1. create()          — raise a PO against a supplier with line items
 *   2. updateStatus()    — send the PO to the supplier (DRAFT → SENT)
 *   3. receivePoLine()   — record goods arriving for a specific line:
 *                          a. Validates quantity does not exceed outstanding
 *                          b. Creates a new Lot (via LotService)
 *                          c. Posts an InventoryTransaction of type RECEIPT
 *                          d. Updates the line's receivedQuantity
 *                          e. Flips PO status to PARTIALLY_RECEIVED or RECEIVED
 *
 * Business rules enforced:
 *   - Only DRAFT orders can be updated or deleted
 *   - Cannot receive more than the outstanding (ordered - received) quantity
 *   - PO status is auto-managed based on receipt progress
 *
 * Called by: PurchaseOrderController
 */
@Service
@RequiredArgsConstructor
@Transactional
public class PurchaseOrderService {

    private final PurchaseOrderRepository purchaseOrderRepository;
    private final SupplierService supplierService;
    private final ProductService productService;
    private final LotService lotService;
    private final InventoryTransactionService transactionService;

    /** Returns all purchase orders. No filtering applied. */
    public List<PurchaseOrder> findAll() {
        return purchaseOrderRepository.findAll();
    }

    /** Finds a PO by ID or throws ResourceNotFoundException (HTTP 404). */
    public PurchaseOrder findById(Long id) {
        return purchaseOrderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Purchase order not found with id: " + id));
    }

    /** Returns all POs for a given supplier — for supplier order history views. */
    public List<PurchaseOrder> findBySupplier(Long supplierId) {
        return purchaseOrderRepository.findBySupplierId(supplierId);
    }

    /** Returns all POs with a given status — for workflow filtering (e.g. all open orders). */
    public List<PurchaseOrder> findByStatus(PurchaseOrder.PurchaseOrderStatus status) {
        return purchaseOrderRepository.findByStatus(status);
    }

    /**
     * Creates a new Purchase Order in DRAFT status.
     * Validates the supplier exists and defaults the order date to today.
     * Lines (PoLine) are persisted via CascadeType.ALL on the entity.
     */
    public PurchaseOrder create(PurchaseOrder po) {
        Supplier supplier = supplierService.findById(po.getSupplier().getSupplierId());
        po.setSupplier(supplier);
        po.setStatus(PurchaseOrder.PurchaseOrderStatus.DRAFT);
        if (po.getOrderDate() == null) po.setOrderDate(LocalDate.now());
        return purchaseOrderRepository.save(po);
    }

    /**
     * Updates editable fields of a DRAFT Purchase Order.
     * Throws BusinessException if the PO is no longer in DRAFT status,
     * since changes to sent or received orders would cause data integrity issues.
     */
    public PurchaseOrder update(Long id, PurchaseOrder updated) {
        PurchaseOrder existing = findById(id);
        if (existing.getStatus() != PurchaseOrder.PurchaseOrderStatus.DRAFT) {
            throw new BusinessException("Only DRAFT purchase orders can be updated");
        }
        existing.setExpectedDate(updated.getExpectedDate());
        if (updated.getSupplier() != null) {
            existing.setSupplier(supplierService.findById(updated.getSupplier().getSupplierId()));
        }
        return purchaseOrderRepository.save(existing);
    }

    /**
     * Manually overrides the status of a PurchaseOrder.
     * Useful for transitions like DRAFT → SENT that don't require business logic.
     * Status transitions with side effects (like receiving) use dedicated methods.
     */
    public PurchaseOrder updateStatus(Long id, PurchaseOrder.PurchaseOrderStatus newStatus) {
        PurchaseOrder po = findById(id);
        po.setStatus(newStatus);
        return purchaseOrderRepository.save(po);
    }

    /**
     * Records the physical receipt of goods against a specific PO line.
     *
     * This is the core inbound inventory method. It:
     *   1. Finds the PO line within the PO
     *   2. Validates the received quantity doesn't exceed what's still outstanding
     *   3. Creates a new Lot via LotService with the supplier, product, cost, and batch info
     *   4. Posts an InventoryTransaction (RECEIPT) which also updates Lot.remainingQuantity
     *   5. Increments the line's receivedQuantity
     *   6. Updates PO status: all lines fully received → RECEIVED, else PARTIALLY_RECEIVED
     *
     * @param poId             the purchase order being received against
     * @param poLineId         the specific line on the PO
     * @param quantityReceived how many units have arrived
     * @param lotNumber        the supplier's batch/lot reference (for traceability)
     * @param expirationDate   optional expiry date (null if product doesn't expire)
     * @return the newly created Lot
     */
    public Lot receivePoLine(Long poId, Long poLineId, int quantityReceived, String lotNumber,
                             LocalDate expirationDate) {
        PurchaseOrder po = findById(poId);

        // Find the specific line on this PO
        PoLine line = po.getPoLines().stream()
                .filter(l -> l.getPoLineId().equals(poLineId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("PO Line not found: " + poLineId));

        // Guard against over-receiving
        int outstanding = line.getOrderedQuantity() - line.getReceivedQuantity();
        if (quantityReceived > outstanding) {
            throw new BusinessException("Cannot receive more than ordered. Outstanding: " + outstanding);
        }

        // Create a new lot for this batch of received goods
        Lot lot = Lot.builder()
                .product(line.getProduct())
                .supplier(po.getSupplier())
                .lotNumber(lotNumber)
                .receivedDate(LocalDate.now())
                .expirationDate(expirationDate)
                .originalQuantity(quantityReceived)
                .remainingQuantity(quantityReceived) // starts fully available
                .unitCost(line.getUnitCost())        // inherit cost from the PO line
                .build();
        lot = lotService.create(lot);

        // Post the RECEIPT transaction — this also decrements/increments lot.remainingQuantity
        InventoryTransaction txn = InventoryTransaction.builder()
                .product(line.getProduct())
                .lot(lot)
                .transactionType(InventoryTransaction.TransactionType.RECEIPT)
                .quantityChange(quantityReceived)     // positive: adding stock
                .unitCost(line.getUnitCost())
                .referenceType(InventoryTransaction.ReferenceType.PURCHASE_ORDER)
                .referenceId(poId)
                .transactionDate(LocalDateTime.now())
                .build();
        transactionService.record(txn);

        // Update the line's running received total
        line.setReceivedQuantity(line.getReceivedQuantity() + quantityReceived);

        // Auto-update PO status based on whether all lines are now fully received
        boolean fullyReceived = po.getPoLines().stream()
                .allMatch(l -> l.getReceivedQuantity() >= l.getOrderedQuantity());
        po.setStatus(fullyReceived
                ? PurchaseOrder.PurchaseOrderStatus.RECEIVED
                : PurchaseOrder.PurchaseOrderStatus.PARTIALLY_RECEIVED);
        purchaseOrderRepository.save(po);

        return lot;
    }

    /**
     * Deletes a DRAFT purchase order.
     * Throws BusinessException if the PO is not in DRAFT status — you cannot
     * delete a PO that has been sent to a supplier or has received stock.
     */
    public void delete(Long id) {
        PurchaseOrder po = findById(id);
        if (po.getStatus() != PurchaseOrder.PurchaseOrderStatus.DRAFT) {
            throw new BusinessException("Only DRAFT purchase orders can be deleted");
        }
        purchaseOrderRepository.deleteById(id);
    }
}
