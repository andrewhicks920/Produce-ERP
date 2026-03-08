package com.andrewhicks.produce_erp.dto;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ProductDTO {
    private Long id;
    private String name;
    private String sku;
    private BigDecimal price;
    private BigDecimal cost;
    private Integer quantity;
}