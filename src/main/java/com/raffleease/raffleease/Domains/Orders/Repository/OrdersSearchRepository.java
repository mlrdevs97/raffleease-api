package com.raffleease.raffleease.Domains.Orders.Repository;

import com.raffleease.raffleease.Domains.Orders.DTOs.OrderSearchFilters;
import com.raffleease.raffleease.Domains.Orders.Model.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface OrdersSearchRepository {
    Page<Order> searchOrders(OrderSearchFilters filters, Long associationId, Pageable pageable);
}