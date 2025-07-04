package com.raffleease.raffleease.Domains.Customers.Controller;

import com.raffleease.raffleease.Domains.Auth.Validations.ValidateAssociationAccess;
import com.raffleease.raffleease.Domains.Customers.DTO.CustomerSearchFilters;
import com.raffleease.raffleease.Domains.Customers.Services.CustomersService;
import com.raffleease.raffleease.Common.Responses.ApiResponse;
import com.raffleease.raffleease.Common.Responses.ResponseFactory;
import com.raffleease.raffleease.Common.RateLimiting.RateLimit;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import static com.raffleease.raffleease.Common.RateLimiting.RateLimit.AccessLevel.PRIVATE;

@ValidateAssociationAccess
@RequiredArgsConstructor
@RestController
@RequestMapping("/v1/associations/{associationId}/customers")
public class CustomersController {
    private final CustomersService customersService;

    @GetMapping
    @RateLimit(operation = "search", accessLevel = PRIVATE)
    public ResponseEntity<ApiResponse> search (
            @PathVariable Long associationId,
            CustomerSearchFilters searchFilters,
            Pageable pageable
    ) {
        return ResponseEntity.ok(
                ResponseFactory.success(
                        customersService.search(associationId, searchFilters, pageable),
                        "Customers retrieved successfully"
                )
        );
    }
}
