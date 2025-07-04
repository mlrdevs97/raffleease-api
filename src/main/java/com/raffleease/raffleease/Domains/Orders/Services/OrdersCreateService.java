package com.raffleease.raffleease.Domains.Orders.Services;

import com.raffleease.raffleease.Domains.Orders.DTOs.OrderCreate;
import com.raffleease.raffleease.Domains.Orders.DTOs.OrderDTO;

public interface OrdersCreateService {
    /**
     * Creates a new order for the tickets in the user's cart.
     * The cart is closed after the order is created, though the tickets remain in RESERVED status until the order is confirmed.
     * A new customer is created during the order creation process.
     * The raffle the tickets belong to must be active to create an order.
     * The order remains in PENDING status until the order is confirmed, cancelled, refunded or unpaid.
     * 
     * @param adminOrder the admin order to create
     * @param associationId the ID of the association
     * @return the created order
     */
    OrderDTO create(OrderCreate adminOrder, Long associationId);
}
