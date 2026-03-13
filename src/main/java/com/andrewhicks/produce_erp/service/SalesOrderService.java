package com.andrewhicks.produce_erp.service;

import com.andrewhicks.produce_erp.exception.BusinessException;
import com.andrewhicks.produce_erp.exception.ResourceNotFoundException;
import com.andrewhicks.produce_erp.model.*;
import com.andrewhicks.produce_erp.repository.SalesOrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Business logic layer for Sales Order management.
 *
 * Sales Orders drive the outbound stock flow. The most critical method here is ship(),
 * which implements FIFO lot consumption — the process of deducting sold goods from
 * the oldest available inventory batches first.
 *
 * Full outbound flow:
 *   1. create()      — raise an order for a customer with line items
 *   2. updateStatus() — confirm the order (DRAFT → CONFIRMED)
 *   3. ship()        — deduct stock using FIFO and mark as SHIPPED:
 *                      For each SalesOrderLine:
 *                        a. Gets available lots ordered oldest-first (LotRepository)
 *                        b. Iterates lots, consuming up to lot.remainingQuantity per lot
 *                        c. Posts an InventoryTransaction (SALE, negative qty) per lot consumed
 *                        d. If stock runs out before filling the line → BusinessException
 *                      PO is marked SHIPPED if all lines fulfilled
 *   4. Invoice is created separately (see InvoiceService)
 *
 * Business rules enforced:
 *   - Only DRAFT orders can be edited or deleted
 *   - Only CONFIRMED orders can be shipped
 *   - Shipment is atomic: if any product has insufficient stock, the entire ship() fails
 *
 * Called by: SalesOrderController
 */
@Service
@RequiredArgsConstructor
@Transactional
public class SalesOrderService {

    private final SalesOrderRepository salesOrderRepository;
    private final CustomerService customerService;
    private final ProductService productService;
    private final LotService lotService;
    private final InventoryTransactionService transactionService;

    /** Returns all sales orders. No filtering applied. */
    public List<SalesOrder> findAll() {
        return salesOrderRepository.findAll();
    }

    /** Finds a sales order by ID or throws ResourceNotFoundException (HTTP 404). */
    public SalesOrder findById(Long id) {
        return salesOrderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sales order not found with id: " + id));
    }

    /** Returns all orders for a given customer — for customer order history views. */
    public List<SalesOrder> findByCustomer(Long customerId) {
        return salesOrderRepository.findByCustomerCustomerId(customerId);
    }

    /** Filters orders by status — for workflow dashboards (e.g. "orders ready to ship"). */
    public List<SalesOrder> findByStatus(SalesOrder.SalesOrderStatus status) {
        return salesOrderRepository.findByStatus(status);
    }

    /**
     * Creates a new Sales Order in DRAFT status.
     * Validates the customer, defaults the order date to today, and auto-calculates
     * the total amount as the sum of (quantity × unitPrice) across all lines.
     * Lines are persisted via CascadeType.ALL on the SalesOrder entity.
     */
    public SalesOrder create(SalesOrder salesOrder) {
        Customer customer = customerService.findById(salesOrder.getCustomer().getCustomerId());
        salesOrder.setCustomer(customer);
        salesOrder.setStatus(SalesOrder.SalesOrderStatus.DRAFT);
        if (salesOrder.getOrderDate() == null) salesOrder.setOrderDate(LocalDate.now());

        // Calculate total amount from line items
        if (salesOrder.getSalesOrderLines() != null) {
            BigDecimal total = salesOrder.getSalesOrderLines().stream()
                    .map(line -> line.getUnitPrice().multiply(BigDecimal.valueOf(line.getQuantity())))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            salesOrder.setTotalAmount(total);
        }
        return salesOrderRepository.save(salesOrder);
    }

    /**
     * Updates editable fields of a DRAFT Sales Order.
     * Throws BusinessException if the order has progressed beyond DRAFT status.
     */
    public SalesOrder update(Long id, SalesOrder updated) {
        SalesOrder existing = findById(id);
        if (existing.getStatus() != SalesOrder.SalesOrderStatus.DRAFT) {
            throw new BusinessException("Only DRAFT sales orders can be updated");
        }
        existing.setOrderDate(updated.getOrderDate());
        if (updated.getCustomer() != null) {
            existing.setCustomer(customerService.findById(updated.getCustomer().getCustomerId()));
        }
        return salesOrderRepository.save(existing);
    }

    /**
     * Manually overrides the status of a SalesOrder.
     * Use for simple transitions like DRAFT → CONFIRMED that don't require additional logic.
     * Transitions with side effects (shipment) use the dedicated ship() method.
     */
    public SalesOrder updateStatus(Long id, SalesOrder.SalesOrderStatus newStatus) {
        SalesOrder so = findById(id);
        so.setStatus(newStatus);
        return salesOrderRepository.save(so);
    }

    /**
     * Ships a CONFIRMED sales order using FIFO lot consumption.
     *
     * This is the core outbound inventory method. For each line on the order:
     *   1. Retrieves available lots for the product, ordered oldest-first (FIFO)
     *   2. Iterates through lots, consuming as much as needed from each lot
     *   3. Posts an InventoryTransaction (SALE, negative quantity) for each lot consumed
     *      — this also calls LotService.adjustRemainingQuantity() to update the lot
     *   4. If stock runs out before the line quantity is fulfilled → BusinessException
     *      (the entire operation rolls back due to @Transactional)
     *
     * After all lines are successfully fulfilled, the order is marked SHIPPED.
     *
     * @throws BusinessException if the order is not CONFIRMED, or if any product
     *                           has insufficient stock to fulfill its ordered quantity
     */
    public SalesOrder ship(Long salesOrderId) {
        SalesOrder so = findById(salesOrderId);
        if (so.getStatus() != SalesOrder.SalesOrderStatus.CONFIRMED) {
            throw new BusinessException("Only CONFIRMED orders can be shipped");
        }

        for (SalesOrderLine line : so.getSalesOrderLines()) {
            int remaining = line.getQuantity(); // units still needed from this line

            // Get available lots ordered by received date (FIFO — oldest first)
            List<Lot> availableLots = lotService.findAvailableLotsByProduct(
                    line.getProduct().getProductId());

            for (Lot lot : availableLots) {
                if (remaining <= 0) break;

                // Take as much from this lot as we need (or all of it if less is available)
                int toConsume = Math.min(remaining, lot.getRemainingQuantity());

                // Post a SALE transaction — this also decrements lot.remainingQuantity
                InventoryTransaction txn = InventoryTransaction.builder()
                        .product(line.getProduct())
                        .lot(lot)
                        .transactionType(InventoryTransaction.TransactionType.SALE)
                        .quantityChange(-toConsume)   // negative: removing stock
                        .unitCost(lot.getUnitCost())  // FIFO cost from this specific lot
                        .referenceType(InventoryTransaction.ReferenceType.SALES_ORDER)
                        .referenceId(salesOrderId)
                        .transactionDate(LocalDateTime.now())
                        .build();
                transactionService.record(txn);
                remaining -= toConsume;
            }

            // If we couldn't fulfil the full quantity, reject the shipment entirely
            if (remaining > 0) {
                throw new BusinessException("Insufficient stock for product: "
                        + line.getProduct().getName() + ". Short by " + remaining);
            }
        }

        so.setStatus(SalesOrder.SalesOrderStatus.SHIPPED);
        return salesOrderRepository.save(so);
    }

    /**
     * Deletes a DRAFT sales order.
     * Throws BusinessException if the order has been confirmed or shipped —
     * you cannot delete an order that has already had stock committed or consumed.
     */
    public void delete(Long id) {
        SalesOrder so = findById(id);
        if (so.getStatus() != SalesOrder.SalesOrderStatus.DRAFT) {
            throw new BusinessException("Only DRAFT sales orders can be deleted");
        }
        salesOrderRepository.deleteById(id);
    }
}
