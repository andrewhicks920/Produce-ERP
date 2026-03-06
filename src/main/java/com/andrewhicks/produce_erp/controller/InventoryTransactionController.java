package com.andrewhicks.produce_erp.controller;

import com.andrewhicks.produce_erp.model.InventoryTransaction;
import com.andrewhicks.produce_erp.repository.InventoryTransactionRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/transactions")
public class InventoryTransactionController {

    private final InventoryTransactionRepository repository;

    public InventoryTransactionController(InventoryTransactionRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<InventoryTransaction> getAllTransactions() {
        return repository.findAll();
    }

    @PostMapping
    public InventoryTransaction createTransaction(@RequestBody InventoryTransaction transaction) {
        return repository.save(transaction);
    }
}