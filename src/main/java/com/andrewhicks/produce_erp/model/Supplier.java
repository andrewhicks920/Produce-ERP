package com.andrewhicks.produce_erp.model;

import jakarta.persistence.*;
import lombok.Data;


@Entity
@Table(name = "supplier")
@Data
public class Supplier {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;



}
