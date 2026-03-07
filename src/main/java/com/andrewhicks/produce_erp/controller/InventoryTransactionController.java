package com.andrewhicks.produce_erp.controller;

import com.andrewhicks.produce_erp.model.InventoryTransaction;
import com.andrewhicks.produce_erp.repository.InventoryTransactionRepository;
import com.andrewhicks.produce_erp.service.InventoryTransactionService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/transactions")
public class InventoryTransactionController {

    private final InventoryTransactionService service;

    public InventoryTransactionController(InventoryTransactionService service) {
        this.service = service;
    }

    @GetMapping
    public List<InventoryTransaction> getAllTransactions() {
        return service.getAllTransactions();
    }

    @PostMapping
    public InventoryTransaction createTransaction(@RequestBody InventoryTransaction transaction) {
        return service.createTransaction(transaction);
    }
}