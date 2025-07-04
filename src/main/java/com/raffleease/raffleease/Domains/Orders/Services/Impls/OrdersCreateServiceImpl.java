package com.raffleease.raffleease.Domains.Orders.Services.Impls;

import com.raffleease.raffleease.Domains.Associations.Model.Association;
import com.raffleease.raffleease.Domains.Associations.Services.AssociationsService;
import com.raffleease.raffleease.Domains.Carts.Model.Cart;
import com.raffleease.raffleease.Domains.Carts.Services.CartsPersistenceService;
import com.raffleease.raffleease.Domains.Carts.Services.CartLifecycleService;
import com.raffleease.raffleease.Domains.Customers.Model.Customer;
import com.raffleease.raffleease.Domains.Customers.Services.CustomersService;
import com.raffleease.raffleease.Domains.Orders.DTOs.OrderCreate;
import com.raffleease.raffleease.Domains.Orders.DTOs.OrderDTO;
import com.raffleease.raffleease.Domains.Orders.Mappers.OrdersMapper;
import com.raffleease.raffleease.Domains.Orders.Model.Order;
import com.raffleease.raffleease.Domains.Orders.Model.OrderItem;
import com.raffleease.raffleease.Domains.Orders.Services.OrdersCreateService;
import com.raffleease.raffleease.Domains.Orders.Services.OrdersPersistenceService;
import com.raffleease.raffleease.Domains.Payments.Model.Payment;
import com.raffleease.raffleease.Domains.Payments.Services.PaymentsService;
import com.raffleease.raffleease.Domains.Raffles.Model.Raffle;
import com.raffleease.raffleease.Domains.Raffles.Model.RaffleStatus;
import com.raffleease.raffleease.Domains.Raffles.Services.RafflesPersistenceService;
import com.raffleease.raffleease.Domains.Raffles.Services.RafflesQueryService;
import com.raffleease.raffleease.Domains.Raffles.Services.RafflesStatisticsService;
import com.raffleease.raffleease.Domains.Tickets.Model.Ticket;
import com.raffleease.raffleease.Domains.Tickets.Services.TicketsQueryService;
import com.raffleease.raffleease.Domains.Notifications.Services.EmailsService;
import com.raffleease.raffleease.Common.Exceptions.CustomExceptions.BusinessException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

import static com.raffleease.raffleease.Domains.Carts.Model.CartStatus.ACTIVE;
import static com.raffleease.raffleease.Domains.Orders.Model.OrderStatus.PENDING;

@RequiredArgsConstructor
@Service
public class OrdersCreateServiceImpl implements OrdersCreateService {
    private final OrdersPersistenceService ordersPersistenceService;
    private final RafflesQueryService rafflesQueryService;
    private final RafflesPersistenceService rafflesPersistence;
    private final RafflesStatisticsService statisticsService;
    private final CartsPersistenceService cartsPersistence;
    private final CustomersService customersService;
    private final TicketsQueryService ticketsQueryService;
    private final PaymentsService paymentsService;
    private final AssociationsService associationsService;
    private final CartLifecycleService cartLifecycleService;
    private final EmailsService emailsService;
    private final OrdersMapper mapper;

    @Override
    @Transactional
    public OrderDTO create(OrderCreate adminOrder, Long associationId) {
        Association association = associationsService.findById(associationId);
        Cart cart = cartsPersistence.findById(adminOrder.cartId());
        List<Ticket> requestedTickets = ticketsQueryService.findAllById(adminOrder.ticketIds());
        validateRequest(requestedTickets, cart, association);
        Raffle raffle = rafflesPersistence.findById(adminOrder.raffleId());
        validateRaffleStatus(raffle);
        Customer customer = customersService.create(adminOrder.customer());        
        List<Ticket> finalizedTickets = cartLifecycleService.finalizeCart(cart, customer);
        statisticsService.setCreateOrderStatistics(raffle, finalizedTickets.stream().count());
        Order order = createOrder(raffle, customer, adminOrder.comment());
        Payment payment = createPayment(order, finalizedTickets);
        List<OrderItem> orderItems = createOrderItems(order, finalizedTickets);
        order.setPayment(payment);
        order.getOrderItems().addAll(orderItems);
        order = ordersPersistenceService.save(order);        
        emailsService.sendOrderCreatedEmail(order);        
        return mapper.fromOrder(order);
    }

    private void validateRequest(List<Ticket> requestedTickets, Cart cart, Association association) {
        List<Ticket> cartTickets = ticketsQueryService.findAllByCart(cart);
        validateCartStatus(cart);
        validateAllTicketsBelongToAssociationRaffle(requestedTickets, association);
        validateAllTicketsBelongToCart(cartTickets, requestedTickets);
        validateAllCartTicketsIncludedInRequest(cartTickets, requestedTickets);
    }

    private void validateCartStatus(Cart cart) {
        if (cart.getStatus() != ACTIVE) {
            throw new BusinessException("Cannot create order for a closed cart");
        }
    }

    private void validateAllTicketsBelongToCart(List<Ticket> cartTickets, List<Ticket> requestedTickets) {
        Set<Long> cartTicketIds = cartTickets.stream()
                .map(Ticket::getId)
                .collect(Collectors.toSet());

        boolean anyNotBelong = requestedTickets.stream()
                .map(Ticket::getId)
                .anyMatch(id -> !cartTicketIds.contains(id));

        if (anyNotBelong) {
            throw new BusinessException("Some tickets do not belong to current cart");
        }
    }

    private void validateAllCartTicketsIncludedInRequest(List<Ticket> cartTickets, List<Ticket> requestedTickets) {
        Set<Long> requestedTicketIds = requestedTickets.stream()
                .map(Ticket::getId)
                .collect(Collectors.toSet());

        boolean anyNotIncluded = cartTickets.stream()
                .map(Ticket::getId)
                .anyMatch(id -> !requestedTicketIds.contains(id));

        if (anyNotIncluded) {
            throw new BusinessException("Some tickets in cart are not included in order request");
        }
    }

    private void validateAllTicketsBelongToAssociationRaffle(List<Ticket> tickets, Association association) {
        Set<Raffle> associationRaffles = new HashSet<>(rafflesQueryService.findAllByAssociation(association));
        Set<Raffle> ticketsRaffles = tickets.stream().map(Ticket::getRaffle).collect(Collectors.toSet());

        boolean anyNotBelong = ticketsRaffles.stream().anyMatch(raffle -> !associationRaffles.contains(raffle));
        if (anyNotBelong) {
            throw new BusinessException("Some tickets do not belong to an association raffle");
        }
    }

    /**
     * Returns the total price of the given tickets, based on their raffle's ticket price.
     * Groups tickets by raffle and sums (ticket price × quantity) for each group.
     *
     * @param tickets the list of tickets to calculate the total for
     * @return the total price as a {@link BigDecimal}
     */
    private BigDecimal calculateOrderTotal(List<Ticket> tickets) {
        return  tickets.stream()
                .collect(Collectors.groupingBy(Ticket::getRaffle, Collectors.counting()))
                .entrySet().stream()
                .map(entry -> entry.getKey().getTicketPrice().multiply(BigDecimal.valueOf(entry.getValue())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private String generateOrderReference() {
        String randomPart = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return "ORD-" + randomPart;
    }

    private Payment createPayment(Order order, List<Ticket> tickets) {
        BigDecimal total = calculateOrderTotal(tickets);
        return paymentsService.create(order, total);
    }

    private Order createOrder(Raffle raffle, Customer customer, String comment) {
        return ordersPersistenceService.save(Order.builder()
                .raffle(raffle)
                .status(PENDING)
                .orderReference(generateOrderReference())
                .customer(customer)
                .orderItems(new ArrayList<>())
                .comment(comment)
                .build());
    }

    private List<OrderItem> createOrderItems(Order order, List<Ticket> tickets) {
        return tickets.stream().map(ticket -> OrderItem.builder()
                .order(order)
                .ticketNumber(ticket.getTicketNumber())
                .priceAtPurchase(ticket.getRaffle().getTicketPrice())
                .ticketId(ticket.getId())
                .raffleId(ticket.getRaffle().getId())
                .customerId(order.getCustomer().getId())
                .build()).toList();
    }

    private void validateRaffleStatus(Raffle raffle) {
        if (raffle.getStatus() != RaffleStatus.ACTIVE) {
            throw new BusinessException("Cannot create order for " + raffle.getStatus() + " raffle");
        }
    }
}
