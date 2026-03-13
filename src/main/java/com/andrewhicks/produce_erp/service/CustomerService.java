package com.andrewhicks.produce_erp.service;

import com.andrewhicks.produce_erp.exception.ResourceNotFoundException;
import com.andrewhicks.produce_erp.model.Customer;
import com.andrewhicks.produce_erp.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Business logic layer for Customer management.
 *
 * Customers are the buyers who place sales orders. This service is intentionally
 * simple — it handles CRUD for customer records and is also called by
 * SalesOrderService to validate a customerId before attaching it to a new order.
 *
 * Called by:
 *   CustomerController (via REST API)
 *   SalesOrderService.create() — to validate and attach the customer
 */
@Service
@RequiredArgsConstructor
@Transactional
public class CustomerService {

    private final CustomerRepository customerRepository;

    /** Returns all customers. No filtering applied. */
    public List<Customer> findAll() {
        return customerRepository.findAll();
    }

    /**
     * Finds a customer by ID or throws ResourceNotFoundException (HTTP 404).
     * Also used internally by SalesOrderService to validate customerId before use.
     */
    public Customer findById(Long id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + id));
    }

    /** Case-insensitive partial name search for customer lookup UIs. */
    public List<Customer> searchByName(String name) {
        return customerRepository.findByNameContainingIgnoreCase(name);
    }

    /** Creates a new customer. No uniqueness constraint applied (add one if needed). */
    public Customer create(Customer customer) {
        return customerRepository.save(customer);
    }

    /** Updates the name of an existing customer. */
    public Customer update(Long id, Customer updated) {
        Customer existing = findById(id);
        existing.setName(updated.getName());
        return customerRepository.save(existing);
    }

    /**
     * Deletes a customer by ID.
     * Note: will fail at the database level if the customer has existing sales orders.
     * Consider soft-deletes for production use.
     */
    public void delete(Long id) {
        findById(id); // ensures 404 is thrown if not found
        customerRepository.deleteById(id);
    }
}
