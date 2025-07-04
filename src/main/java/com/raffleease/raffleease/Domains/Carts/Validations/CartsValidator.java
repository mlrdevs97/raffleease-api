package com.raffleease.raffleease.Domains.Carts.Validations;

import com.raffleease.raffleease.Common.Exceptions.CustomExceptions.AuthorizationException;
import com.raffleease.raffleease.Common.Exceptions.CustomExceptions.BusinessException;
import com.raffleease.raffleease.Domains.Associations.Model.Association;
import com.raffleease.raffleease.Domains.Carts.Model.Cart;
import com.raffleease.raffleease.Domains.Raffles.Model.Raffle;
import com.raffleease.raffleease.Domains.Raffles.Services.RafflesQueryService;
import com.raffleease.raffleease.Domains.Tickets.Model.Ticket;
import com.raffleease.raffleease.Domains.Users.Model.User;
import com.raffleease.raffleease.Domains.Users.Services.UsersService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.raffleease.raffleease.Domains.Carts.Model.CartStatus.ACTIVE;
import static com.raffleease.raffleease.Domains.Tickets.Model.TicketStatus.AVAILABLE;

@RequiredArgsConstructor
@Component
public class CartsValidator {
    private final UsersService usersService;
    private final RafflesQueryService rafflesQueryService;

    public void validateIsUserCart(Cart cart) {
        User user = usersService.getAuthenticatedUser();
        if (cart.getUser().getId() != user.getId()) {
            throw new AuthorizationException("You are not allowed to access this cart");
        }
    }

    public void validateCartIsActive(Cart cart) {
        if (!cart.getStatus().equals(ACTIVE)) {
            throw new BusinessException("Cart must be ACTIVE to reserve or release tickets");
        }
    }

    public void validateTicketsBelongToAssociationRaffle(List<Ticket> tickets, Association association) {
        Set<Raffle> associationRaffles = new HashSet<>(rafflesQueryService.findAllByAssociation(association));
        Set<Raffle> ticketsRaffles = tickets.stream().map(Ticket::getRaffle).collect(Collectors.toSet());

        boolean anyNotBelong = ticketsRaffles.stream().anyMatch(raffle -> !associationRaffles.contains(raffle));
        if (anyNotBelong) {
            throw new BusinessException("Some tickets do not belong to an association raffle");
        }
    }

    public void validateTicketsAvailability(List<Ticket> tickets) {
        boolean anyUnavailable = tickets.stream().anyMatch(ticket -> ticket.getStatus() != AVAILABLE);
        if (anyUnavailable) {
            throw new BusinessException("Some tickets are not available");
        }
    }

    public void validateTicketsBelongToCart(Cart cart, List<Ticket> tickets) {
        if (cart.getTickets() == null || cart.getTickets().isEmpty()) {
            throw new BusinessException("Cart has no tickets to release");
        }
        
        Set<Long> cartTicketIds = cart.getTickets().stream()
                .map(Ticket::getId)
                .collect(Collectors.toSet());
        
        List<Long> invalidTicketIds = tickets.stream()
                .map(Ticket::getId)
                .filter(id -> !cartTicketIds.contains(id))
                .toList();
                
        if (!invalidTicketIds.isEmpty()) {
            throw new BusinessException("Cannot release tickets that do not belong to the cart. Invalid ticket IDs: " + invalidTicketIds);
        }
    }
}

