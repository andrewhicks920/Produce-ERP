package com.andrewhicks.produce_erp.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
public class Warehouse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private String location;
}