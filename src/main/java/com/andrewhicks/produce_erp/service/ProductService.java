package com.andrewhicks.produce_erp.service;

import com.andrewhicks.produce_erp.model.Product;
import com.andrewhicks.produce_erp.repository.ProductRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public Product createProduct(Product product) {
        return productRepository.save(product);
    }

    public List<Product> getAllProducts() {
        return productRepository.findAll(Sort.by(Sort.Direction.ASC, "id"));
    }

    public Product getProductById(Long id) {
        return productRepository.findById(id)
               .orElseThrow(() -> new RuntimeException("Product not found"));
    }

    public Product updateProduct(Long id, Product updatedProduct) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        product.setName(updatedProduct.getName());
        product.setSku(updatedProduct.getSku());
        product.setPrice(updatedProduct.getPrice());
        product.setCost(updatedProduct.getCost());
        product.setQuantity(updatedProduct.getQuantity());
        product.setSupplier(updatedProduct.getSupplier());

        return productRepository.save(product);
    }

    public void deleteProduct(Long id) {
        productRepository.deleteById(id);
    }
}