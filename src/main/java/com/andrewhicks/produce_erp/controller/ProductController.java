package com.andrewhicks.produce_erp.controller;

import com.andrewhicks.produce_erp.model.Lot;
import com.andrewhicks.produce_erp.model.Product;
import com.andrewhicks.produce_erp.service.LotService;
import com.andrewhicks.produce_erp.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST controller for Product management.
 *
 * Products are the core entities of the inventory system. This controller exposes
 * CRUD endpoints for managing products, plus useful inventory-level sub-resources
 * like current stock count and lot breakdown.
 *
 * Base URL: /api/products
 *
 * Endpoints:
 *   GET    /api/products                       → list all (filter by ?name= or ?category=)
 *   GET    /api/products/{id}                  → get one product
 *   GET    /api/products/sku/{sku}             → find by SKU
 *   GET    /api/products/{id}/stock            → total on-hand quantity (sum of lot remainders)
 *   GET    /api/products/{id}/lots             → all lots (?availableOnly=true for non-zero only)
 *   POST   /api/products                       → create a new product
 *   PUT    /api/products/{id}                  → update an existing product
 *   DELETE /api/products/{id}                  → delete a product
 *
 * All business logic is delegated to ProductService and LotService.
 * Errors (404, 400) are handled globally by GlobalExceptionHandler.
 */
@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;
    private final LotService lotService;

    /**
     * Returns all products. Supports optional filtering:
     *   ?name=widget     → case-insensitive partial name match
     *   ?category=raw    → exact category match
     * Only one filter is applied at a time (name takes priority).
     */
    @GetMapping
    public ResponseEntity<List<Product>> findAll(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String category) {
        if (name != null) return ResponseEntity.ok(productService.searchByName(name));
        if (category != null) return ResponseEntity.ok(productService.findByCategory(category));
        return ResponseEntity.ok(productService.findAll());
    }

    /** Returns a single product by its internal ID. Returns 404 if not found. */
    @GetMapping("/{id}")
    public ResponseEntity<Product> findById(@PathVariable Long id) {
        return ResponseEntity.ok(productService.findById(id));
    }

    /** Returns a single product by its unique SKU. Returns 404 if no match. */
    @GetMapping("/sku/{sku}")
    public ResponseEntity<Product> findBySku(@PathVariable String sku) {
        return ResponseEntity.ok(productService.findBySku(sku));
    }

    /**
     * Returns the total on-hand stock for a product.
     * Calculated by summing remainingQuantity across all active lots.
     * Response: { "productId": 1, "productName": "Widget", "totalStock": 150 }
     */
    @GetMapping("/{id}/stock")
    public ResponseEntity<Map<String, Object>> getStock(@PathVariable Long id) {
        Product product = productService.findById(id);
        Integer stock = productService.getTotalStock(id);
        return ResponseEntity.ok(Map.of(
                "productId", id,
                "productName", product.getName(),
                "totalStock", stock
        ));
    }

    /**
     * Returns the lots for a product.
     *   ?availableOnly=false (default) → all lots including exhausted ones (full history)
     *   ?availableOnly=true            → only lots with stock remaining (FIFO selection list)
     */
    @GetMapping("/{id}/lots")
    public ResponseEntity<List<Lot>> getLots(@PathVariable Long id,
            @RequestParam(defaultValue = "false") boolean availableOnly) {
        if (availableOnly) {
            return ResponseEntity.ok(lotService.findAvailableLotsByProduct(id));
        }
        return ResponseEntity.ok(lotService.findByProduct(id));
    }

    /** Creates a new product. Returns 400 if the SKU already exists. */
    @PostMapping
    public ResponseEntity<Product> create(@RequestBody Product product) {
        return ResponseEntity.status(HttpStatus.CREATED).body(productService.create(product));
    }

    /** Updates an existing product. Returns 400 if the new SKU conflicts with another product. */
    @PutMapping("/{id}")
    public ResponseEntity<Product> update(@PathVariable Long id, @RequestBody Product product) {
        return ResponseEntity.ok(productService.update(id, product));
    }

    /** Deletes a product. Will fail if the product has associated lots or transactions. */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        productService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
