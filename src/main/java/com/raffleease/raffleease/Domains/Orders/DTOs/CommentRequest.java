package com.raffleease.raffleease.Domains.Orders.DTOs;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;

@Builder
public record CommentRequest(
        @NotNull
        @Size(max = 500)
        String comment
) {}
