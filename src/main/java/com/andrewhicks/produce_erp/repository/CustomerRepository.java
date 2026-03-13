package com.andrewhicks.produce_erp.repository;

import com.andrewhicks.produce_erp.model.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

/**
 * Data access layer for Customer entities.
 *
 * Customers are the starting point of the sales chain. This repository provides
 * standard CRUD plus a name search for customer lookup UIs.
 *
 * Used by: CustomerService
 * Related: each Customer has many SalesOrders
 */
@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {

    /**
     * Case-insensitive partial name search for customer lookup and autocomplete.
     * Translates to: WHERE LOWER(name) LIKE LOWER('%name%')
     */
    List<Customer> findByNameContainingIgnoreCase(String name);
}
