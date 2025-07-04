package com.raffleease.raffleease.Domains.Carts.Controllers;

import com.raffleease.raffleease.Domains.Auth.Validations.ValidateAssociationAccess;
import com.raffleease.raffleease.Domains.Carts.DTO.CartDTO;
import com.raffleease.raffleease.Domains.Carts.Services.CartsService;
import com.raffleease.raffleease.Domains.Carts.DTO.ReservationRequest;
import com.raffleease.raffleease.Domains.Carts.Services.ReservationsService;
import com.raffleease.raffleease.Common.Responses.ApiResponse;
import com.raffleease.raffleease.Common.Responses.ResponseFactory;
import com.raffleease.raffleease.Common.RateLimiting.RateLimit;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

import static com.raffleease.raffleease.Common.RateLimiting.RateLimit.AccessLevel.PRIVATE;

@ValidateAssociationAccess
@RequiredArgsConstructor
@RestController
@RequestMapping("/v1/associations/{associationId}/carts")
public class CartsController {
    private final CartsService cartsService;
    private final ReservationsService reservationsService;

    @PostMapping
    @RateLimit(operation = "create", accessLevel = PRIVATE)
    public ResponseEntity<ApiResponse> create() {
        CartDTO createdCart = cartsService.create();

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(createdCart.id())
                .toUri();

        return ResponseEntity.created(location).body(
                ResponseFactory.success(
                        createdCart,
                        "New cart created successfully"
                )
        );
    }

    @GetMapping("/active")
    @RateLimit(operation = "read", accessLevel = PRIVATE)
    public ResponseEntity<ApiResponse> getUserActiveCart() {
        return ResponseEntity.ok(
                ResponseFactory.success(
                        cartsService.getUserActiveCart(),
                        "Active user cart retrieved successfully"
                )
        );
    }

    @GetMapping("/{cartId}")
    @RateLimit(operation = "read", accessLevel = PRIVATE)
    public ResponseEntity<ApiResponse> get(
            @PathVariable Long cartId
    ) {
        return ResponseEntity.ok(
                ResponseFactory.success(
                        cartsService.get(cartId),
                        "Cart retrieved successfully"
                )
        );
    }

    @PostMapping("/{cartId}/reservations")
    @RateLimit(operation = "create", accessLevel = PRIVATE)
    public ResponseEntity<ApiResponse> reserve(
            @PathVariable Long associationId,
            @PathVariable Long cartId,
            @Valid @RequestBody ReservationRequest reservationRequest
    ) {
        return ResponseEntity.ok(
                ResponseFactory.success(
                        reservationsService.reserve(reservationRequest, associationId, cartId),
                        "New reservation generated successfully"
                )
        );
    }

    @PutMapping("/{cartId}/reservations")
    @RateLimit(operation = "update", accessLevel = PRIVATE)
    public ResponseEntity<ApiResponse> release(
            @PathVariable Long associationId,
            @PathVariable Long cartId,
            @Valid @RequestBody ReservationRequest request
    ) {
        reservationsService.release(request, associationId, cartId);
        return ResponseEntity.ok().body(ResponseFactory.success("Tickets released successfully"));
    }
}