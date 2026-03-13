package com.andrewhicks.produce_erp.service;

import com.andrewhicks.produce_erp.exception.BusinessException;
import com.andrewhicks.produce_erp.exception.ResourceNotFoundException;
import com.andrewhicks.produce_erp.model.Product;
import com.andrewhicks.produce_erp.repository.LotRepository;
import com.andrewhicks.produce_erp.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Business logic layer for Product management.
 *
 * Products are the central entities of the inventory system — every purchasing
 * and sales operation ultimately references a product. This service handles CRUD
 * for products and also provides stock-level queries by delegating to LotRepository.
 *
 * The SKU (Stock Keeping Unit) uniqueness check is enforced here at the service level
 * in addition to the database constraint, to provide a clear error message rather
 * than a cryptic database exception.
 *
 * Called by:
 *   ProductController (via REST API)
 *   PurchaseOrderService — to resolve productId on PoLines
 *   SalesOrderService — to resolve productId on SalesOrderLines
 *   LotService — to resolve and attach a product when creating a new lot
 *   InventoryAdjustmentService — to resolve productId on AdjustmentLines
 */
@Service
@RequiredArgsConstructor
@Transactional
public class ProductService {

    private final ProductRepository productRepository;
    private final LotRepository lotRepository;

    /** Returns all products. No filtering applied. */
    public List<Product> findAll() {
        return productRepository.findAll();
    }

    /**
     * Finds a product by its internal ID or throws ResourceNotFoundException (HTTP 404).
     * Used extensively by other services to validate FKs before use.
     */
    public Product findById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
    }

    /** Finds a product by its unique SKU or throws ResourceNotFoundException. */
    public Product findBySku(String sku) {
        return productRepository.findBySku(sku)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with SKU: " + sku));
    }

    /** Returns all products in a given category (exact match, case-sensitive). */
    public List<Product> findByCategory(String category) {
        return productRepository.findByCategory(category);
    }

    /** Case-insensitive partial name search for product autocomplete and filtering. */
    public List<Product> searchByName(String name) {
        return productRepository.findByNameContainingIgnoreCase(name);
    }

    /**
     * Returns the total on-hand stock for a product by summing remainingQuantity
     * across all its active lots. Returns 0 if no lots exist.
     *
     * Exposed via GET /api/products/{id}/stock
     */
    public Integer getTotalStock(Long productId) {
        findById(productId); // validates product exists first
        Integer stock = lotRepository.getTotalStockByProduct(productId);
        return stock != null ? stock : 0;
    }

    /**
     * Creates a new product after validating SKU uniqueness.
     * Throws BusinessException if another product with the same SKU already exists.
     */
    public Product create(Product product) {
        if (productRepository.findBySku(product.getSku()).isPresent()) {
            throw new BusinessException("A product with SKU '" + product.getSku() + "' already exists");
        }
        return productRepository.save(product);
    }

    /**
     * Updates an existing product. Validates SKU uniqueness if the SKU has changed —
     * it's fine to keep the same SKU, but changing to one that's already taken is rejected.
     */
    public Product update(Long id, Product updated) {
        Product existing = findById(id);
        if (!existing.getSku().equals(updated.getSku()) &&
                productRepository.findBySku(updated.getSku()).isPresent()) {
            throw new BusinessException("A product with SKU '" + updated.getSku() + "' already exists");
        }
        existing.setName(updated.getName());
        existing.setSku(updated.getSku());
        existing.setCategory(updated.getCategory());
        existing.setUnitOfMeasure(updated.getUnitOfMeasure());
        existing.setCostMethod(updated.getCostMethod());
        return productRepository.save(existing);
    }

    /**
     * Deletes a product by ID.
     * Note: if the product has associated lots or transactions, the database
     * will throw a constraint violation. Consider soft-deletes for production use.
     */
    public void delete(Long id) {
        findById(id); // ensures 404 is thrown if not found
        productRepository.deleteById(id);
    }
}
