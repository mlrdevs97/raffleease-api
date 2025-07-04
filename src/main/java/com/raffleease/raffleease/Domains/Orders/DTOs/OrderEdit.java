package com.raffleease.raffleease.Domains.Orders.DTOs;

import com.raffleease.raffleease.Domains.Carts.Model.Cart;
import com.raffleease.raffleease.Domains.Customers.Model.Customer;
import com.raffleease.raffleease.Domains.Payments.Model.Payment;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record OrderEdit(
        Cart cart,
        Payment payment,
        Customer customer,
        LocalDateTime orderDate
) {
}
