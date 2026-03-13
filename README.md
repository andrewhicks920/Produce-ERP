# Produce ERP – Inventory & Order Management System

A full-stack backend ERP system built for produce distribution and wholesale operations. The application manages the complete supply chain lifecycle — from supplier purchase orders and lot-based inventory receipt through customer sales orders, invoicing, and payment collection.

---

## Table of Contents

- [Overview](#overview)
- [Tech Stack](#tech-stack)
- [Domain Model](#domain-model)
- [Key Features](#key-features)
- [Getting Started](#getting-started)
- [Project Structure](#project-structure)
- [API Overview](#api-overview)

---

## Overview

Produce ERP models the core operational workflows of a produce wholesaler or distributor. Inventory is managed at the lot level, enabling full traceability from supplier receipt to customer fulfillment. All stock movements are recorded as immutable inventory transactions, providing a complete audit trail for every quantity change in the system.

The costing model follows **FIFO (First In, First Out)**, where the oldest available lots are consumed first during sales fulfillment — a standard approach in perishable goods industries where expiration dates directly impact cost accuracy.

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 17 |
| Framework | Spring Boot 4.0 |
| Persistence | Spring Data JPA / Hibernate |
| Database | PostgreSQL |
| Build Tool | Maven |
| Utilities | Lombok |

---

## Domain Model

The system is organized around the following core entities:

**Procurement**
- `Supplier` — vendor records with contact information
- `PurchaseOrder` — orders placed with suppliers, progressing through `DRAFT → SENT → PARTIALLY_RECEIVED → RECEIVED`
- `PoLine` — individual line items on a purchase order, tracking ordered and received quantities

**Inventory**
- `Product` — catalog of items with SKU, category, unit of measure, and cost method
- `Lot` — a distinct batch of received goods tied to a supplier and product, with expiration tracking and a running remaining quantity
- `InventoryTransaction` — immutable ledger of every stock movement (`RECEIPT`, `SALE`, `ADJUSTMENT`, `RETURN`), referencing the source document
- `InventoryAdjustment` / `AdjustmentLine` — manual corrections to inventory with reason codes and author tracking

**Sales**
- `Customer` — buyer records
- `SalesOrder` — customer orders progressing through `DRAFT → CONFIRMED → PARTIALLY_SHIPPED → SHIPPED → INVOICED`
- `SalesOrderLine` — individual line items with quantity and unit price

**Billing**
- `Invoice` — invoices generated against fulfilled sales orders, tracking `DRAFT → SENT → PARTIALLY_PAID → PAID` status
- `Payment` — individual payment records against an invoice with payment method tracking

---

## Key Features

- **Lot-based inventory tracking** — every unit of stock is tied to a specific lot with a received date, expiration date, and unit cost
- **FIFO costing** — lots are consumed in the order they were received, ensuring accurate cost of goods sold for perishable inventory
- **Full transaction ledger** — every inventory movement (receipt, sale, adjustment, return) is recorded with its source reference, creating an immutable audit trail
- **Multi-status order workflows** — both purchase orders and sales orders follow realistic status progressions that reflect partial fulfillment scenarios
- **Expiration date visibility** — lots are queryable by expiration date, supporting first-expired-first-out operations and waste reduction
- **Invoice and payment lifecycle** — invoices are tracked from draft through full payment, with partial payment status support

---

## Getting Started

### Prerequisites

- Java 17+
- PostgreSQL 14+
- Maven 3.8+

### Database Setup

Create the database:

```sql
CREATE DATABASE produce_erp;
```

Configure your credentials in `src/main/resources/application.properties`:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/produce_erp
spring.datasource.username=your_username
spring.datasource.password=your_password
spring.jpa.hibernate.ddl-auto=update
```

Hibernate will auto-generate the schema on first run.

To seed the database with test data:

```bash
psql -U your_username -d produce_erp -f src/main/resources/test-data.sql
```

### Running the Application

```bash
mvn spring-boot:run
```

The application starts on `http://localhost:8080` by default.

---

## Project Structure

```
src/main/java/com/andrewhicks/produce_erp/
├── controller/        # REST controllers (request handling & routing)
├── service/           # Business logic layer
├── repository/        # Spring Data JPA repositories
├── model/             # JPA entities
└── exception/         # Custom exception types
```

---

## API Overview

| Resource | Base Path |
|---|---|
| Products | `/api/products` |
| Suppliers | `/api/suppliers` |
| Purchase Orders | `/api/purchase-orders` |
| Customers | `/api/customers` |
| Sales Orders | `/api/sales-orders` |
| Inventory Transactions | `/api/inventory-transactions` |
| Invoices | `/api/invoices` |
| Payments | `/api/payments` |
| Inventory Adjustments | `/api/adjustments` |

All endpoints follow standard REST conventions and return JSON.