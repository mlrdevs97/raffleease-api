package com.raffleease.raffleease.Domains.Orders.DTOs;

import com.raffleease.raffleease.Domains.Customers.DTO.CustomerCreate;
import jakarta.annotation.Nullable;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Builder;

import java.util.List;

@Builder
public record OrderCreate(
        @NotNull
        @Positive
        Long cartId,

        @NotNull
        @Positive
        Long raffleId,

        @NotEmpty
        List<Long> ticketIds,

        @NotNull
        @Valid
        CustomerCreate customer,

        @Nullable
        @Size(max = 500)
        String comment
) {
        public OrderCreate {
                comment = trim(comment);
        }

        private static String trim(String value) {
                return value == null ? null : value.trim();
        }
}