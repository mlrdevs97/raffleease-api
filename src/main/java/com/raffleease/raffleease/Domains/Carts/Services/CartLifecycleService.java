package com.raffleease.raffleease.Domains.Carts.Services;

import com.raffleease.raffleease.Domains.Carts.Model.Cart;
import com.raffleease.raffleease.Domains.Customers.Model.Customer;
import com.raffleease.raffleease.Domains.Tickets.Model.Ticket;

import java.util.List;

/**
 * Service responsible for managing cart lifecycle operations including
 * release, closure, and finalization in a consistent manner across all scenarios.
 */
public interface CartLifecycleService {
    
    /**
     * Releases an expired or abandoned cart, making all reserved tickets available again.
     * Used by scheduled cleanup and when creating new carts.
     * 
     * @param cart the cart to release
     */
    void releaseCart(Cart cart);
    
    /**
     * Finalizes a cart by transferring reserved tickets to a customer and closing the cart.
     * Used during order creation process.
     * 
     * @param cart the cart to finalize
     * @param customer the customer to assign tickets to
     * @return the list of finalized tickets
     */
    List<Ticket> finalizeCart(Cart cart, Customer customer);
    
    /**
     * Releases expired carts in batch.
     * Optimized for scheduled cleanup operations.
     * 
     * @param expiredCarts list of carts to release
     */
    void releaseExpiredCarts(List<Cart> expiredCarts);
} 