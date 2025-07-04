package com.raffleease.raffleease.Domains.Carts.Services.Impl;

import com.raffleease.raffleease.Domains.Associations.Model.Association;
import com.raffleease.raffleease.Domains.Associations.Services.AssociationsService;
import com.raffleease.raffleease.Domains.Carts.DTO.CartDTO;
import com.raffleease.raffleease.Domains.Carts.Mappers.CartsMapper;
import com.raffleease.raffleease.Domains.Carts.Model.Cart;
import com.raffleease.raffleease.Domains.Carts.Services.CartsPersistenceService;
import com.raffleease.raffleease.Domains.Carts.Validations.CartsValidator;
import com.raffleease.raffleease.Domains.Carts.DTO.ReservationRequest;
import com.raffleease.raffleease.Domains.Carts.Services.ReservationsService;
import com.raffleease.raffleease.Domains.Raffles.Services.RafflesQueryService;
import com.raffleease.raffleease.Domains.Raffles.Services.RafflesStatisticsService;
import com.raffleease.raffleease.Domains.Tickets.Model.Ticket;
import com.raffleease.raffleease.Domains.Tickets.Services.TicketsQueryService;
import com.raffleease.raffleease.Domains.Tickets.Services.TicketsService;
import com.raffleease.raffleease.Domains.Users.Services.UsersService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@RequiredArgsConstructor
@Service
public class ReservationsServiceImpl implements ReservationsService {
    private final TicketsQueryService ticketsQueryService;
    private final AssociationsService associationsService;
    private final RafflesQueryService rafflesQueryService;
    private final CartsPersistenceService cartsPersistenceService;
    private final CartsMapper cartsMapper;
    private final TicketsService ticketsService;
    private final RafflesStatisticsService statisticsService;
    private final UsersService usersService;
    private final CartsValidator cartsValidator;

    @Override
    @Transactional
    public CartDTO reserve(ReservationRequest request, Long associationId, Long cartId) {
        Cart cart = cartsPersistenceService.findById(cartId);
        cartsValidator.validateIsUserCart(cart);
        cartsValidator.validateCartIsActive(cart);
        Association association = associationsService.findById(associationId);
        List<Ticket> tickets = ticketsQueryService.findAllById(request.ticketIds());
        cartsValidator.validateTicketsBelongToAssociationRaffle(tickets, association);
        cartsValidator.validateTicketsAvailability(tickets);
        reserveTickets(cart, tickets);
        cart.getTickets().addAll(tickets);
        Cart savedCart = cartsPersistenceService.save(cart);
        return cartsMapper.fromCart(savedCart);
    }

    @Override
    @Transactional
    public void release(ReservationRequest request, Long associationId, Long cartId) {
        Cart cart = cartsPersistenceService.findById(cartId);
        cartsValidator.validateIsUserCart(cart);
        cartsValidator.validateCartIsActive(cart);
        Association association = associationsService.findById(associationId);
        List<Ticket> tickets = ticketsQueryService.findAllById(request.ticketIds());
        cartsValidator.validateTicketsBelongToAssociationRaffle(tickets, association);
        cartsValidator.validateTicketsBelongToCart(cart, tickets);
        ticketsService.releaseTickets(tickets);
        statisticsService.increaseRafflesTicketsAvailability(tickets);
        cart.getTickets().removeAll(tickets);
        cartsPersistenceService.save(cart);
    }

    private void reserveTickets(Cart cart, List<Ticket> tickets) {
        ticketsService.reserveTickets(cart, tickets);
        statisticsService.reduceRaffleTicketsAvailability(tickets);
    }
}