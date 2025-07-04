package com.raffleease.raffleease.Domains.Orders.Services.Impls;

import com.raffleease.raffleease.Domains.Orders.Model.Order;
import com.raffleease.raffleease.Domains.Orders.Repository.OrdersRepository;
import com.raffleease.raffleease.Domains.Orders.Services.OrdersPersistenceService;
import com.raffleease.raffleease.Common.Exceptions.CustomExceptions.DatabaseException;
import com.raffleease.raffleease.Common.Exceptions.CustomExceptions.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class OrdersPersistenceServiceImpl implements OrdersPersistenceService {
    private final OrdersRepository repository;

    @Override
    public Order findById(Long id) {
        try {
            return repository.findById(id).orElseThrow(() -> new NotFoundException("Order not found for id <" + id + ">"));
        } catch (DataAccessException ex) {
            throw new DatabaseException("Database error occurred while fetching order with ID <" + id + ">: " + ex.getMessage());
        }
    }

    @Override
    public Order save(Order order) {
        try {
            return repository.save(order);
        } catch (DataAccessException ex) {
            throw new DatabaseException("Database error occurred while saving order: " + ex.getMessage());
        }
    }
}
