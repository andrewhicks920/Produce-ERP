package com.andrewhicks.produce_erp.service;

import com.andrewhicks.produce_erp.model.InventoryTransaction;
import com.andrewhicks.produce_erp.model.Product;
import com.andrewhicks.produce_erp.repository.InventoryTransactionRepository;
import com.andrewhicks.produce_erp.repository.ProductRepository;
import org.springframework.stereotype.Service;


import java.util.List;

@Service
public class InventoryTransactionService {

    private final InventoryTransactionRepository transactionRepository;
    private final ProductRepository productRepository;

    public InventoryTransactionService(InventoryTransactionRepository transactionRepository,  ProductRepository productRepository) {
        this.transactionRepository = transactionRepository;
        this.productRepository = productRepository;
    }

    public List<InventoryTransaction> getAllTransactions() {
        return transactionRepository.findAll();
    }


    /*
    Given an InventoryTransaction,

    PURCHASE →   +quantity
    SALE →       -quantity
    ADJUSTMENT → set value

    This +/-/set will pertain to the specific Product

    So far, no error checking on if there's a case where a Product doesn't have
    enough to satisfy a transaction. For example, if I have 40 apples and a
    Transaction asks for 50, I will now have -10 apples ¯\_(ツ)_/¯
     */
    public InventoryTransaction createTransaction(InventoryTransaction transaction) {
        Product product = productRepository.findById(transaction.getProduct().getId())
                .orElseThrow(() -> new RuntimeException("Product not found"));

        int currentQuantity = product.getQuantity();
        int transactionQuantity = transaction.getQuantity();

        switch (transaction.getType()) {

            case PURCHASE:
                product.setQuantity(currentQuantity + transactionQuantity);
                break;

            case SALE:
                if (currentQuantity < transactionQuantity)
                    throw new RuntimeException("Not enough inventory!");

                product.setQuantity(currentQuantity - transactionQuantity);
                break;

            case ADJUSTMENT:
                product.setQuantity(transactionQuantity);
                break;
        }

        productRepository.save(product);
        transaction.setProduct(product);

        return transactionRepository.save(transaction);
    }
}