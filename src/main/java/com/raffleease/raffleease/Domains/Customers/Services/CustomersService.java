package com.raffleease.raffleease.Domains.Customers.Services;

import com.raffleease.raffleease.Domains.Customers.DTO.CustomerCreate;
import com.raffleease.raffleease.Domains.Customers.DTO.CustomerDTO;
import com.raffleease.raffleease.Domains.Customers.DTO.CustomerSearchFilters;
import com.raffleease.raffleease.Domains.Customers.Model.Customer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CustomersService {
    /**
     * Creates a new customer.
     * Customers are created when a new order is created.
     * 
     * @param customerData the customer data
     * @return the created customer
     */
    Customer create(CustomerCreate customerData);

    /**
     * Searches for customers based on the provided filters and paginates the results.
     * The search is performed on the customers of the association.
     * 
     * @param associationId the association ID
     * @param searchFilters the search filters
     * @param pageable the pageable
     * @return the page of customers
     */
    Page<CustomerDTO> search(Long associationId, CustomerSearchFilters searchFilters, Pageable pageable);
}