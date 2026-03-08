package com.andrewhicks.produce_erp.model;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;

@Entity
@Table(name = "products")
@Data
public class Product {

    // Don't be an idiot like me and put an ID attribute in the POST request
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(unique = true, nullable = false)
    private String sku;

    @Column
    private BigDecimal price; // How much to sell Product for

    @Column
    private BigDecimal cost; // How much to pay to acquire Product

    @Column(nullable = false)
    private Integer quantity; // Current inventory level

}
