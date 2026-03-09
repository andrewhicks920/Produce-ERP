package com.andrewhicks.produce_erp.model;

import jakarta.persistence.*;
import lombok.Data;

import java.util.List;


@Entity
@Table(name = "supplier")
@Data
public class Supplier {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Company or trading name of given Supplier
    private String name;

    // The name of primary contact @ this Supplier
    @Column(name = "contact_name")
    private String contactName;

     // All purchase orders raised against this supplier.
     // Lazily loaded to avoid fetching full order history on every Supplier lookup
    @OneToMany(mappedBy = "supplier", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<PurchaseOrder> purchaseOrders;


    // All inventory lots that originated from this supplier.
    // Used for supplier-level traceability (e.g. product recall management).
    // @OneToMany(mappedBy = "supplier", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    // private List<Lot> lots;

    // Obvipusly, this hasnt been added yet — but it will soon!

}
