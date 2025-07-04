package com.raffleease.raffleease.Domains.Customers.Services.Impl;

import com.raffleease.raffleease.Domains.Customers.DTO.CustomerCreate;
import com.raffleease.raffleease.Domains.Customers.DTO.CustomerDTO;
import com.raffleease.raffleease.Domains.Customers.DTO.CustomerSearchFilters;
import com.raffleease.raffleease.Domains.Customers.Mappers.CustomersMapper;
import com.raffleease.raffleease.Domains.Customers.Model.Customer;
import com.raffleease.raffleease.Domains.Customers.Repository.CustomersSearchRepository;
import com.raffleease.raffleease.Domains.Customers.Repository.CustomersRepository;
import com.raffleease.raffleease.Domains.Customers.Services.CustomersService;
import com.raffleease.raffleease.Common.Exceptions.CustomExceptions.DatabaseException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class CustomersServiceImpl implements CustomersService {
    private final CustomersRepository repository;
    private final CustomersSearchRepository customRepository;
    private final CustomersMapper mapper;

    @Override
    public Customer create(CustomerCreate data) {
        return save(mapper.toCustomer(data));
    }

    @Override
    public Page<CustomerDTO> search(Long associationId, CustomerSearchFilters searchFilters, Pageable pageable) {
        Page<Customer> customersPage = customRepository.search(searchFilters, associationId, pageable);
        return customersPage.map(mapper::fromCustomer);
    }

    private Customer save(Customer customer) {
        try {
            return repository.save(customer);
        } catch (DataAccessException ex) {
            throw new DatabaseException("Database error occurred while saving new customer: " + ex.getMessage());
        }
    }
}
