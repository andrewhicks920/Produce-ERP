package com.andrewhicks.produce_erp.repository;

import com.andrewhicks.produce_erp.model.InventoryAdjustment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Data access layer for InventoryAdjustment entities.
 *
 * InventoryAdjustments are manual stock corrections grouped under a header record.
 * This repository supports audit-style queries: who made adjustments, and when.
 *
 * Used by: InventoryAdjustmentService
 * Related: each InventoryAdjustment has many AdjustmentLines (children)
 */
@Repository
public interface InventoryAdjustmentRepository extends JpaRepository<InventoryAdjustment, Long> {

    /**
     * Returns all adjustments made by a specific user.
     * Useful for audit trails — "show me everything changed by user X".
     */
    List<InventoryAdjustment> findByCreatedBy(String createdBy);

    /**
     * Returns adjustments created within a date/time window.
     * Used for period-based audit reporting and stock reconciliation.
     */
    List<InventoryAdjustment> findByCreatedDateBetween(LocalDateTime from, LocalDateTime to);
}
