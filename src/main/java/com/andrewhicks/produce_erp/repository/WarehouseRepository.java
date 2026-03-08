package com.andrewhicks.produce_erp.repository;

import com.andrewhicks.produce_erp.model.Warehouse;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WarehouseRepository extends JpaRepository<Warehouse, Long> {
}