package com.raffleease.raffleease.Domains.Images.DTOs;

import lombok.*;

@Builder
public record ImageFile(
        byte[] data,
        String contentType
) {
}
