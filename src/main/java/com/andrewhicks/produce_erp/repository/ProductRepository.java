package com.andrewhicks.produce_erp.repository;

import com.andrewhicks.produce_erp.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

/**
 * Data access layer for Product entities.
 *
 * Products are the central entity of the inventory system, referenced by
 * PoLine, SalesOrderLine, Lot, and InventoryTransaction. This repository
 * supports lookups by the unique SKU field and by category for filtering.
 *
 * Used by: ProductService, PurchaseOrderService, SalesOrderService, LotService
 */
@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    /**
     * Looks up a product by its unique SKU identifier.
     * Returns Optional.empty() if not found, allowing the service to throw
     * a descriptive ResourceNotFoundException.
     */
    Optional<Product> findBySku(String sku);

    /**
     * Filters products by category (exact match, case-sensitive).
     * Used for category-level stock views and reporting.
     */
    List<Product> findByCategory(String category);

    /**
     * Case-insensitive partial name search for product lookup UIs and autocomplete.
     * Translates to: WHERE LOWER(name) LIKE LOWER('%name%')
     */
    List<Product> findByNameContainingIgnoreCase(String name);
}
