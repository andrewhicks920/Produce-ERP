package com.andrewhicks.produce_erp.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Data
public class InventoryTransaction {
    public enum TransactionType {
        PURCHASE,
        SALE,
        ADJUSTMENT
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne
    private Warehouse warehouse;

    private Integer quantity;

    @Enumerated(EnumType.STRING)
    private TransactionType type; // PURCHASE, SALE, ADJUSTMENT

    private LocalDateTime transactionDate = LocalDateTime.now();
}