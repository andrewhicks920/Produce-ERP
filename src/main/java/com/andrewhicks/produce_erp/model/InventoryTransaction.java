package com.andrewhicks.produce_erp.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Data
public class InventoryTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    private Integer quantity;

    private String type; // PURCHASE, SALE, ADJUSTMENT

    private LocalDateTime transactionDate = LocalDateTime.now();
}