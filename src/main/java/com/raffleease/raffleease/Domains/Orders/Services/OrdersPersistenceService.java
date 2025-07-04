package com.raffleease.raffleease.Domains.Orders.Services;

import com.raffleease.raffleease.Domains.Orders.Model.Order;

public interface OrdersPersistenceService {
    /**
     * Finds an order by its ID.
     * 
     * @param id the ID of the order to find
     * @return the found order
     */
    Order findById(Long id);
    
    /**
     * Saves an order.
     * 
     * @param entity the order to save
     * @return the saved order
     */
    Order save(Order entity);
}