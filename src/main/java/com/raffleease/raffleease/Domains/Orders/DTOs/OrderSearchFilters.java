package com.raffleease.raffleease.Domains.Orders.DTOs;

import com.raffleease.raffleease.Domains.Orders.Model.OrderStatus;
import com.raffleease.raffleease.Domains.Payments.Model.PaymentMethod;
import com.raffleease.raffleease.Common.Validations.ValidOrderSearchFilters;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static com.raffleease.raffleease.Common.Utils.SanitizeUtils.trim;
import static com.raffleease.raffleease.Common.Utils.SanitizeUtils.trimAndLower;

@ValidOrderSearchFilters
public record OrderSearchFilters(
        OrderStatus status,
        PaymentMethod paymentMethod,
        String orderReference,

        @Size(max = 100)
        String customerName,

        @Size(max = 100)
        String customerEmail,

        @Size(max = 30)
        String customerPhone,

        @Positive
        Long raffleId,

        @PositiveOrZero
        BigDecimal minTotal,

        @PositiveOrZero
        BigDecimal maxTotal,

        LocalDateTime createdFrom,
        LocalDateTime createdTo,
        LocalDateTime completedFrom,
        LocalDateTime completedTo,
        LocalDateTime cancelledFrom,
        LocalDateTime cancelledTo
) {
        public OrderSearchFilters {
                orderReference = trim(orderReference);
                customerName = trim(customerName);
                customerEmail = trimAndLower(customerEmail);
                customerPhone = trim(customerPhone);
        }
}
