package com.raffleease.raffleease.Domains.Orders.Controller;

import com.raffleease.raffleease.Domains.Auth.Validations.ValidateAssociationAccess;
import com.raffleease.raffleease.Domains.Auth.Validations.RequireRole;
import com.raffleease.raffleease.Domains.Orders.DTOs.*;
import com.raffleease.raffleease.Domains.Orders.Services.OrdersCreateService;
import com.raffleease.raffleease.Domains.Orders.Services.OrdersEditService;
import com.raffleease.raffleease.Common.Responses.ApiResponse;
import com.raffleease.raffleease.Common.Responses.ResponseFactory;
import com.raffleease.raffleease.Domains.Orders.Services.OrdersQueryService;
import com.raffleease.raffleease.Common.RateLimiting.RateLimit;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

import static com.raffleease.raffleease.Common.RateLimiting.RateLimit.AccessLevel.PRIVATE;
import static com.raffleease.raffleease.Domains.Associations.Model.AssociationRole.MEMBER;

@ValidateAssociationAccess
@RequiredArgsConstructor
@RestController
@RequestMapping("/v1/associations/{associationId}/orders")
public class OrdersController {
    private final OrdersQueryService ordersQueryService;
    private final OrdersCreateService ordersCreateService;
    private final OrdersEditService ordersEditService;

    @PostMapping
    @RateLimit(operation = "create", accessLevel = PRIVATE)
    public ResponseEntity<ApiResponse> create(
            @PathVariable Long associationId,
            @Valid @RequestBody OrderCreate orderCreate
    ) {
        OrderDTO order = ordersCreateService.create(orderCreate, associationId);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(order.id())
                .toUri();

        return ResponseEntity.created(location).body(
                ResponseFactory.success(
                        order,
                        "New order created successfully"
                )
        );
    }

    @GetMapping
    @RateLimit(operation = "search", accessLevel = PRIVATE)
    public ResponseEntity<ApiResponse> search(
            @PathVariable Long associationId,
            @Valid OrderSearchFilters filters,
            Pageable pageable
    ) {
        return ResponseEntity.ok(ResponseFactory.success(
                ordersQueryService.search(filters, associationId, pageable),
                "Orders retrieved successfully")
        );
    }

    @GetMapping("/{orderId}")
    @RateLimit(operation = "read", accessLevel = PRIVATE)
    public ResponseEntity<ApiResponse> get(
            @PathVariable Long orderId
    ) {
        return ResponseEntity.ok(ResponseFactory.success(
                ordersQueryService.get(orderId),
                "Order retrieved successfully"
        ));
    }

    @PutMapping("/{orderId}/complete")
    @RateLimit(operation = "update", accessLevel = PRIVATE)
    public ResponseEntity<ApiResponse> complete(
            @PathVariable Long orderId,
            @Valid @RequestBody OrderComplete orderComplete
    ) {
        return ResponseEntity.ok(ResponseFactory.success(
                ordersEditService.complete(orderId, orderComplete),
                "Order completed successfully"
        ));
    }

    @PutMapping("/{orderId}/cancel")
    @RateLimit(operation = "update", accessLevel = PRIVATE)
    public ResponseEntity<ApiResponse> cancel(
            @PathVariable Long orderId
    ) {
        return ResponseEntity.ok(ResponseFactory.success(
                ordersEditService.cancel(orderId),
                "Order cancelled successfully"
        ));
    }

    @PutMapping("/{orderId}/refund")
    @RequireRole(value = MEMBER, message = "Only administrators and members can refund orders")
    @RateLimit(operation = "update", accessLevel = PRIVATE)
    public ResponseEntity<ApiResponse> refund(
            @PathVariable Long orderId
    ) {
        return ResponseEntity.ok(ResponseFactory.success(
                ordersEditService.refund(orderId),
                "Order refunded successfully"
        ));
    }

    @PutMapping("/{orderId}/unpaid")
    @RequireRole(value = MEMBER, message = "Only administrators and members can set orders as unpaid")
    @RateLimit(operation = "update", accessLevel = PRIVATE)
    public ResponseEntity<ApiResponse> setUnpaid(
            @PathVariable Long orderId
    ) {
        return ResponseEntity.ok(ResponseFactory.success(
                ordersEditService.setUnpaid(orderId),
                "Order unpaid successfully"
        ));
    }

    @PostMapping("/{orderId}/comment")
    @RateLimit(operation = "create", accessLevel = PRIVATE)
    public ResponseEntity<ApiResponse> addComment(
            @PathVariable Long orderId,
            @Valid @RequestBody CommentRequest request
    ) {
        return ResponseEntity.ok(ResponseFactory.success(
                ordersEditService.addComment(orderId, request),
                "Order comment added successfully"
        ));
    }

    @PutMapping("/{orderId}/comment")
    @RateLimit(operation = "test", accessLevel = PRIVATE)
    public ResponseEntity<ApiResponse> editComment(
            @PathVariable Long orderId,
            @Valid @RequestBody CommentRequest request
    ) {
        return ResponseEntity.ok(ResponseFactory.success(
                ordersEditService.addComment(orderId, request),
                "Order comment edited successfully"
        ));
    }

    @DeleteMapping("/{orderId}/comment")
    @RateLimit(operation = "delete", accessLevel = PRIVATE)
    public ResponseEntity<ApiResponse> removeComment(
            @PathVariable Long orderId
    ) {
        ordersEditService.deleteComment(orderId);
        return ResponseEntity.noContent().build();
    }
}