package com.raffleease.raffleease.Domains.Orders.DTOs;

import com.raffleease.raffleease.Domains.Customers.DTO.CustomerDTO;
import com.raffleease.raffleease.Domains.Orders.Model.OrderStatus;
import com.raffleease.raffleease.Domains.Payments.DTOs.PaymentDTO;
import com.raffleease.raffleease.Domains.Raffles.DTOs.OrderRaffleSummary;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

@Builder
public record OrderDTO(
        Long id,
        OrderRaffleSummary raffleSummary,
        String orderReference,
        OrderStatus status,
        List<OrderItemDTO> orderItems,
        PaymentDTO payment,
        CustomerDTO customer,
        String comment,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime completedAt,
        LocalDateTime cancelledAt
) { }
