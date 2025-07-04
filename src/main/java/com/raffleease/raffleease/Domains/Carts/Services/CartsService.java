package com.raffleease.raffleease.Domains.Carts.Services;

import com.raffleease.raffleease.Common.Exceptions.CustomExceptions.NotFoundException;
import com.raffleease.raffleease.Domains.Carts.DTO.CartDTO;
import com.raffleease.raffleease.Domains.Carts.Model.Cart;

public interface CartsService {
    /**
     * Creates a new cart for the authenticated user.
     * If the user has an active cart, it will be closed and a new one will be created.
     * Creating a cart is the first step in the sell process since it will be used to store the tickets.
     * 
     * @return the created cart
     */
    CartDTO create();

    /**
     * Saves a cart instance in the database.
     * 
     * @param cart the cart to save
     * @return the saved cart
     */
    Cart save(Cart cart);

    /**
     * Gets a cart by its ID.
     * Only the cart owner can get the cart with the provided ID.
     * 
     * @param cartId the ID of the cart
     * @return the cart
     */
    CartDTO get(Long cartId);

    /**
     * Gets the active cart of the authenticated user.
     * If the user has no active cart, an exception will be thrown.
     * 
     * @return the user active cart
     * @throws NotFoundException if the user has no active cart
     */
    CartDTO getUserActiveCart();
}
