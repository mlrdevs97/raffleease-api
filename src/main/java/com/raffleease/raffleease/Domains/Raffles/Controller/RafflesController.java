package com.raffleease.raffleease.Domains.Raffles.Controller;

import com.raffleease.raffleease.Domains.Auth.Validations.ValidateAssociationAccess;
import com.raffleease.raffleease.Domains.Auth.Validations.RequireRole;
import com.raffleease.raffleease.Domains.Raffles.DTOs.RaffleCreate;
import com.raffleease.raffleease.Domains.Raffles.DTOs.RaffleDTO;
import com.raffleease.raffleease.Domains.Raffles.DTOs.RaffleEdit;
import com.raffleease.raffleease.Domains.Raffles.DTOs.StatusUpdate;
import com.raffleease.raffleease.Domains.Raffles.DTOs.RaffleSearchFilters;
import com.raffleease.raffleease.Domains.Raffles.Services.*;
import com.raffleease.raffleease.Common.Responses.ApiResponse;
import com.raffleease.raffleease.Common.Responses.ResponseFactory;
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
@RequestMapping("/v1/associations/{associationId}/raffles")
public class RafflesController {
    private final RafflesCreateService rafflesCreateService;
    private final RafflesEditService rafflesEditService;
    private final RafflesStatusService rafflesStatusService;
    private final RafflesQueryService rafflesQueryService;

    @PostMapping
    @RequireRole(value = MEMBER, message = "Only administrators and members can create raffles")
    @RateLimit(operation = "create", accessLevel = PRIVATE)
    public ResponseEntity<ApiResponse> create(
            @PathVariable Long associationId,
            @RequestBody @Valid RaffleCreate raffleData
    ) {
        RaffleDTO createdRaffle = rafflesCreateService.create(associationId, raffleData);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(createdRaffle.id())
                .toUri();

        return ResponseEntity.created(location).body(
                ResponseFactory.success(
                        createdRaffle,
                        "New raffle created successfully"
                )
        );
    }

    @PutMapping("/{id}")
    @RequireRole(value = MEMBER, message = "Only administrators and members can edit raffles")
    @RateLimit(operation = "update", accessLevel = PRIVATE)
    public ResponseEntity<ApiResponse> edit(
            @PathVariable Long id,
            @RequestBody @Valid RaffleEdit raffleEdit
    ) {
        return ResponseEntity.ok(ResponseFactory.success(
                rafflesEditService.edit(id, raffleEdit),
                "Raffle edited successfully"
        ));
    }

    @PatchMapping("/{id}/status")
    @RequireRole(value = MEMBER, message = "Only administrators and members can update raffle status")
    @RateLimit(operation = "update", accessLevel = PRIVATE)
    public ResponseEntity<ApiResponse> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody StatusUpdate request
    ) {
        return ResponseEntity.ok(ResponseFactory.success(
                rafflesStatusService.updateStatus(id, request),
                "Raffle status updated successfully"
        ));
    }

    @GetMapping("/{id}")
    @RateLimit(operation = "read", accessLevel = PRIVATE)
    public ResponseEntity<ApiResponse> get(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(ResponseFactory.success(
                rafflesQueryService.get(id),
                "Raffle retrieved successfully"
        ));
    }

    @GetMapping
    @RateLimit(operation = "search", accessLevel = RateLimit.AccessLevel.PUBLIC)
    public ResponseEntity<ApiResponse> search(
            @PathVariable Long associationId,
            RaffleSearchFilters searchFilters,
            Pageable pageable
    ) {
        return ResponseEntity.ok(ResponseFactory.success(
                rafflesQueryService.search(associationId, searchFilters, pageable),
                "Raffles retrieved successfully"
        ));
    }

    @DeleteMapping("/{id}")
    @RequireRole(value = MEMBER, message = "Only administrators and members can delete raffles")
    @RateLimit(operation = "delete", accessLevel = PRIVATE)
    public ResponseEntity<Void> delete(
            @PathVariable Long id
    ) {
        rafflesStatusService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
