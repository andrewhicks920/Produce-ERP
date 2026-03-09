package com.andrewhicks.produce_erp.controller;

import com.andrewhicks.produce_erp.model.Product;
import com.andrewhicks.produce_erp.service.ProductService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController // Tells Spring this class handles HTTP API requests
@RequestMapping("/products")
public class ProductController {
    private final ProductService service;

    public ProductController(ProductService service) {
        this.service = service;
    }

    @GetMapping
    public List<Product> getProducts() {
        return service.getAllProducts();
    }

    @PostMapping
    public Product createProduct(@RequestBody Product product) {
        return service.createProduct(product);
    }

    @GetMapping("/{id}")
    public Product getProductByID(@PathVariable Long id) {
        return service.getProductById(id);
    }

    @PutMapping("/{id}")
    public Product updateProductByID(@PathVariable Long id, @RequestBody Product product) {
        return service.updateProduct(id, product);
    }

    @DeleteMapping("/{id}")
    public void deleteProductByID(@PathVariable Long id) {
        service.deleteProduct(id);
    }
}