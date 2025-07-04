package com.raffleease.raffleease.Domains.Orders.Mappers.Impls;

import com.raffleease.raffleease.Domains.Customers.Mappers.CustomersMapper;
import com.raffleease.raffleease.Domains.Orders.DTOs.OrderDTO;
import com.raffleease.raffleease.Domains.Orders.DTOs.OrderItemDTO;
import com.raffleease.raffleease.Domains.Orders.Mappers.OrdersMapper;
import com.raffleease.raffleease.Domains.Orders.Model.Order;
import com.raffleease.raffleease.Domains.Orders.Model.OrderItem;
import com.raffleease.raffleease.Domains.Payments.Mappers.IPaymentsMapper;
import com.raffleease.raffleease.Domains.Raffles.DTOs.OrderRaffleSummary;
import com.raffleease.raffleease.Domains.Raffles.Model.Raffle;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;

@RequiredArgsConstructor
@Service
public class OrdersMapperImpl implements OrdersMapper {
    private final CustomersMapper customersMapper;
    private final IPaymentsMapper paymentsMapper;

    public OrderDTO fromOrder(Order order) {
        return OrderDTO.builder()
                .id(order.getId())
                .raffleSummary(buildRaffleSummary(order.getRaffle()))
                .status(order.getStatus())
                .orderReference(order.getOrderReference())
                .orderItems(fromOrderItemList(order.getOrderItems()))
                .customer(customersMapper.fromCustomer(order.getCustomer()))
                .payment(paymentsMapper.fromPayment(order.getPayment()))
                .comment(order.getComment())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .completedAt(order.getCompletedAt())
                .cancelledAt(order.getCancelledAt())
                .build();
    }

    private List<OrderItemDTO> fromOrderItemList(List<OrderItem> orderItems) {
        return orderItems.stream().map(orderItem -> OrderItemDTO.builder()
                .id(orderItem.getId())
                .priceAtPurchase(orderItem.getPriceAtPurchase())
                .ticketNumber(orderItem.getTicketNumber())
                .raffleId(orderItem.getRaffleId())
                .ticketId(orderItem.getTicketId())
                .customerId(orderItem.getCustomerId())
                .build()
        ).toList();
    }

    private OrderRaffleSummary buildRaffleSummary(Raffle raffle) {
        String imageUrl = "";
        if (raffle.getImages() != null && !raffle.getImages().isEmpty()) {
            imageUrl = raffle.getImages().get(0).getUrl();
        }
        
        return OrderRaffleSummary.builder()
                .id(raffle.getId())
                .title(raffle.getTitle())
                .imageURL(imageUrl)
                .status(raffle.getStatus())
                .build();
    }
}
