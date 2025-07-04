package com.raffleease.raffleease.Domains.Carts.Services.Impl;

import com.raffleease.raffleease.Domains.Carts.Model.Cart;
import com.raffleease.raffleease.Domains.Carts.Services.CartLifecycleService;
import com.raffleease.raffleease.Domains.Carts.Services.CartsPersistenceService;
import com.raffleease.raffleease.Domains.Customers.Model.Customer;
import com.raffleease.raffleease.Domains.Raffles.Services.RafflesStatisticsService;
import com.raffleease.raffleease.Domains.Tickets.Model.Ticket;
import com.raffleease.raffleease.Domains.Tickets.Services.TicketsQueryService;
import com.raffleease.raffleease.Domains.Tickets.Services.TicketsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

import static com.raffleease.raffleease.Domains.Carts.Model.CartStatus.CLOSED;

@Slf4j
@RequiredArgsConstructor
@Service
public class CartLifecycleServiceImpl implements CartLifecycleService {
    private final CartsPersistenceService cartsPersistenceService;
    private final TicketsQueryService ticketsQueryService;
    private final TicketsService ticketsService;
    private final RafflesStatisticsService statisticsService;

    @Override
    public void releaseCart(Cart cart) {
        ticketsService.releaseTickets(cart.getTickets());
        statisticsService.increaseRafflesTicketsAvailability(cart.getTickets());
        closeCart(cart);
    }

    @Override
    public List<Ticket> finalizeCart(Cart cart, Customer customer) {
        List<Ticket> cartTickets = ticketsQueryService.findAllByCart(cart);
        closeCart(cart);
        return ticketsService.transferTicketsToCustomer(cartTickets, customer);
    }

    @Override
    public void releaseExpiredCarts(List<Cart> expiredCarts) {
        log.info("Releasing {} expired carts", expiredCarts.size());
        
        for (Cart cart : expiredCarts) {
            try {
                releaseCart(cart);
            } catch (Exception e) {
                log.error("Failed to release expired cart with ID: {}. Error: {}", cart.getId(), e.getMessage(), e);
            }
        }
        
        log.info("Completed releasing expired carts");
    }

    /**
     * Closes the cart by setting status to CLOSED and clearing ticket associations.
     */
    private void closeCart(Cart cart) {
        log.info("Closing cart with ID: {} - changing status from {} to CLOSED", cart.getId(), cart.getStatus());

        cart.setStatus(CLOSED);
        cart.setTickets(null);
        cartsPersistenceService.save(cart);

        log.info("Cart with ID: {} successfully closed", cart.getId());
    }
} 