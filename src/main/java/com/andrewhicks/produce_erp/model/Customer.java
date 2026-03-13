package com.andrewhicks.produce_erp.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.List;

/**
 * Represents a customer — a buyer who places sales orders.
 *
 * Customers are the starting point of the sales chain:
 *   Customer → SalesOrder → SalesOrderLine → (on ship) → InventoryTransaction
 *                        → Invoice → Payment
 *
 * This is intentionally a lightweight entity. Extend it with fields such as
 * email, phone, billing address, or credit limit as your business requires.
 *
 * Database table: customer
 */
@Entity
@Table(name = "customer")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Customer {

    /** Primary key, auto-incremented by the database. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "customer_id")
    private Long customerId;

    /** The customer's company or personal name. */
    @Column(nullable = false)
    private String name;

    /**
     * All sales orders placed by this customer.
     * Lazily loaded to avoid fetching large order histories unnecessarily.
     */
    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<SalesOrder> salesOrders;
}
