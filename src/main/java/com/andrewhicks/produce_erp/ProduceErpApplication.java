package com.andrewhicks.produce_erp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;


/**
 * Entry point for the Inventory Management System.
 *
 * This application manages the full lifecycle of inventory, purchasing, and sales:
 *
 * Suppliers → Purchase Orders → PO Lines → Lot creation on receipt
 * Products → Lots → Inventory Transactions (FIFO cost tracking)
 * Customers → Sales Orders → Shipment (FIFO lot consumption) → Invoices → Payments
 * Inventory Adjustments for manual stock corrections
 *
 * The project follows a layered architecture:
 *   Controller  → handles HTTP requests and responses
 *   Service     → contains business logic and transaction management
 *   Repository  → handles database access via Spring Data JPA
 *   Model       → JPA entities that map to database tables
 *
 */
@SpringBootApplication
public class ProduceErpApplication {

	public static void main(String[] args) {
		SpringApplication.run(ProduceErpApplication.class, args);
	}

}
