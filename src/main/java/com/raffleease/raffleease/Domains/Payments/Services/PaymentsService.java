package com.raffleease.raffleease.Domains.Payments.Services;

import com.raffleease.raffleease.Domains.Orders.Model.Order;
import com.raffleease.raffleease.Domains.Payments.Model.Payment;

import java.math.BigDecimal;

public interface PaymentsService {
    /**
     * Creates a new payment for an order.
     * A payment is created when an order is completed.
     * 
     * @param order the order to create the payment for
     * @param total the total amount of the payment
     * @return the created payment
     */
    Payment create(Order order, BigDecimal total);
}