package com.raffleease.raffleease.Domains.Orders.DTOs;

import com.raffleease.raffleease.Domains.Payments.Model.PaymentMethod;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

@Builder
public record OrderComplete(
        @NotNull
        PaymentMethod paymentMethod
) { }
