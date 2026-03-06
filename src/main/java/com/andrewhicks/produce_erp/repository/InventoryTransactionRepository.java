package com.andrewhicks.produce_erp.repository;

import com.andrewhicks.produce_erp.model.InventoryTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InventoryTransactionRepository extends JpaRepository<InventoryTransaction, Long> {
}