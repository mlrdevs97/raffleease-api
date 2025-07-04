package com.raffleease.raffleease.Domains.Carts.Services.Impl;

import com.raffleease.raffleease.Common.Exceptions.CustomExceptions.DatabaseException;
import com.raffleease.raffleease.Common.Exceptions.CustomExceptions.NotFoundException;
import com.raffleease.raffleease.Domains.Carts.Model.Cart;
import com.raffleease.raffleease.Domains.Carts.Repository.CartsRepository;
import com.raffleease.raffleease.Domains.Carts.Services.CartsPersistenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class CartsPersistenceServiceImpl implements CartsPersistenceService {
    
    private final CartsRepository repository;

    @Override
    public Cart save(Cart cart) {
        try {
            return repository.save(cart);
        } catch (DataAccessException ex) {
            throw new DatabaseException("Database error occurred while saving cart: " + ex.getMessage());
        }
    }

    @Override
    public Cart findById(Long id) {
        try {
            return repository.findById(id).orElseThrow(() -> 
                new NotFoundException("Cart not found for id <" + id + ">"));
        } catch (DataAccessException ex) {
            throw new DatabaseException("Database error occurred while fetching cart with ID <" + id + ">: " + ex.getMessage());
        }
    }
} 