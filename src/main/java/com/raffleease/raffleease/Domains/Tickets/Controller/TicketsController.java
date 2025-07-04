package com.raffleease.raffleease.Domains.Tickets.Controller;

import com.raffleease.raffleease.Domains.Auth.Validations.ValidateAssociationAccess;
import com.raffleease.raffleease.Domains.Tickets.DTO.*;
import com.raffleease.raffleease.Domains.Tickets.Services.TicketsQueryService;
import com.raffleease.raffleease.Common.Responses.ApiResponse;
import com.raffleease.raffleease.Common.Responses.ResponseFactory;
import com.raffleease.raffleease.Common.RateLimiting.RateLimit;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.raffleease.raffleease.Common.RateLimiting.RateLimit.AccessLevel.PRIVATE;

@ValidateAssociationAccess
@RequiredArgsConstructor
@RestController
@RequestMapping("/v1/associations/{associationId}/raffles/{raffleId}/tickets")
public class TicketsController {
    private final TicketsQueryService queryService;

    @GetMapping
    @RateLimit(operation = "search", accessLevel = PRIVATE,
               message = "Too many search requests. Please try again later.")
    public ResponseEntity<ApiResponse> search(
            @PathVariable Long associationId,
            @PathVariable Long raffleId,
            TicketsSearchFilters searchFilters,
            Pageable pageable
    ) {
        return ResponseEntity.ok(
                ResponseFactory.success(
                        queryService.search(associationId, raffleId, searchFilters, pageable),
                        "Ticket retrieved successfully"
                )
        );
    }

     @GetMapping("/random")
     @RateLimit(operation = "read", accessLevel = PRIVATE,
                message = "Too many random ticket requests. Please try again later.")
     public ResponseEntity<ApiResponse> getRandom(
             @PathVariable Long raffleId,
             @RequestParam Long quantity
     ) {
         List<TicketDTO> tickets = queryService.getRandom(raffleId, quantity);
         return ResponseEntity.ok(
                 ResponseFactory.success(
                         tickets,
                         "Random tickets retrieved successfully"
                 )
         );
     }
}