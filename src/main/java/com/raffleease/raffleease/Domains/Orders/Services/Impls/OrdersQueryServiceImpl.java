package com.raffleease.raffleease.Domains.Orders.Services.Impls;

import com.raffleease.raffleease.Domains.Orders.DTOs.OrderDTO;
import com.raffleease.raffleease.Domains.Orders.DTOs.OrderSearchFilters;
import com.raffleease.raffleease.Domains.Orders.Mappers.OrdersMapper;
import com.raffleease.raffleease.Domains.Orders.Model.Order;
import com.raffleease.raffleease.Domains.Orders.Repository.OrdersRepository;
import com.raffleease.raffleease.Domains.Orders.Services.OrdersPersistenceService;
import com.raffleease.raffleease.Domains.Orders.Services.OrdersQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class OrdersQueryServiceImpl implements OrdersQueryService {
    private final OrdersRepository repository;
    private final OrdersPersistenceService ordersPersistenceService;
    private final OrdersMapper ordersMapper;

    @Override
    public OrderDTO get(Long id) {
        return ordersMapper.fromOrder(ordersPersistenceService.findById(id));
    }

    @Override
    public Page<OrderDTO> search(OrderSearchFilters filters, Long associationId, Pageable pageable) {
        Page<Order> ordersPage = repository.searchOrders(filters, associationId, pageable);
        return ordersPage.map(ordersMapper::fromOrder);
    }
}
