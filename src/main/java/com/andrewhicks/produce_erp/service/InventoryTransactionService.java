package com.andrewhicks.produce_erp.service;

import com.andrewhicks.produce_erp.model.InventoryTransaction;
import com.andrewhicks.produce_erp.repository.InventoryTransactionRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class InventoryTransactionService {

    private final InventoryTransactionRepository repository;

    public InventoryTransactionService(InventoryTransactionRepository repository) {
        this.repository = repository;
    }

    public List<InventoryTransaction> getAllTransactions() {
        return repository.findAll();
    }

    public InventoryTransaction createTransaction(InventoryTransaction transaction) {
        return repository.save(transaction);
    }
}