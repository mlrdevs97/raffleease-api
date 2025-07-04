package com.raffleease.raffleease.Domains.Orders.Mappers;

import com.raffleease.raffleease.Domains.Orders.DTOs.OrderDTO;
import com.raffleease.raffleease.Domains.Orders.Model.Order;
import org.springframework.stereotype.Service;

@Service
public interface OrdersMapper {
    OrderDTO fromOrder(Order order);
}
