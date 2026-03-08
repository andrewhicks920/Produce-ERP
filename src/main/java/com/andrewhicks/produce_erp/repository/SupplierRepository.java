package com.andrewhicks.produce_erp.repository;

import com.andrewhicks.produce_erp.model.Supplier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


@Repository
public interface SupplierRepository extends JpaRepository<Supplier, Long> {

}