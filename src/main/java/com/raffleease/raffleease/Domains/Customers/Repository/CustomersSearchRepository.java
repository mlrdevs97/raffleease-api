package com.raffleease.raffleease.Domains.Customers.Repository;

import com.raffleease.raffleease.Domains.Customers.DTO.CustomerSearchFilters;
import com.raffleease.raffleease.Domains.Customers.Model.Customer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CustomersSearchRepository {
    Page<Customer> search(CustomerSearchFilters searchFilters, Long associationId, Pageable pageable);
}
