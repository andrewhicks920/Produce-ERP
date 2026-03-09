package com.andrewhicks.produce_erp.repository;

import com.andrewhicks.produce_erp.model.Supplier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface SupplierRepository extends JpaRepository<Supplier, Long> {

    /**
     * Case-insensitive name search for supplier lookup UIs.
     * Translates to: WHERE LOWER(name) LIKE LOWER('%name%')
     */
    List<Supplier> findByNameContainingIgnoreCase(String name);
}
