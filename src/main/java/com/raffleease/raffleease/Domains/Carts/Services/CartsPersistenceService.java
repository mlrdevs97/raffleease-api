package com.raffleease.raffleease.Domains.Carts.Services;

import com.raffleease.raffleease.Domains.Carts.Model.Cart;

/**
 * Service responsible for cart persistence operations only.
 * This service exists to break circular dependencies between cart-related services.
 * 
 * Use this when you only need to save/persist cart state without business logic.
 */
public interface CartsPersistenceService {
    /**
     * Saves a cart entity to the database.
     * 
     * @param cart the cart to save
     * @return the saved cart
     */
    Cart save(Cart cart);
    
    /**
     * Finds a cart by its ID.
     * 
     * @param id the cart ID
     * @return the cart
     * @throws com.raffleease.raffleease.Common.Exceptions.CustomExceptions.NotFoundException if cart not found
     */
    Cart findById(Long id);
} 