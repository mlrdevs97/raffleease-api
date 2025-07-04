package com.raffleease.raffleease.Domains.Customers.DTO;

import static com.raffleease.raffleease.Common.Utils.SanitizeUtils.trim;
import static com.raffleease.raffleease.Common.Utils.SanitizeUtils.trimAndLower;

public record CustomerSearchFilters(
    String fullName,
    String email,
    String phoneNumber
) {
    public CustomerSearchFilters {
        fullName = trim(fullName);
        email = trimAndLower(email);
        phoneNumber = trim(phoneNumber);
    }
}
