package com.andrewhicks.produce_erp.model;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;

@Entity
@Table(name = "products")
@Data
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(unique = true, nullable = false)
    private String sku;

    @Column
    private BigDecimal price; // How much you sell the product for

    @Column
    private BigDecimal cost; // How much you pay to acquire the product

    @Column
    private BigDecimal quantity;

}
