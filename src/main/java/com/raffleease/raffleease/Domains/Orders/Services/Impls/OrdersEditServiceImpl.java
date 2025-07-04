package com.raffleease.raffleease.Domains.Orders.Services.Impls;

import com.raffleease.raffleease.Domains.Orders.DTOs.*;
import com.raffleease.raffleease.Domains.Orders.Mappers.OrdersMapper;
import com.raffleease.raffleease.Domains.Orders.Model.Order;
import com.raffleease.raffleease.Domains.Orders.Model.OrderItem;
import com.raffleease.raffleease.Domains.Orders.Model.OrderStatus;
import com.raffleease.raffleease.Domains.Orders.Services.OrdersPersistenceService;
import com.raffleease.raffleease.Domains.Orders.Services.OrdersEditService;
import com.raffleease.raffleease.Domains.Payments.Model.Payment;
import com.raffleease.raffleease.Domains.Raffles.Model.Raffle;
import com.raffleease.raffleease.Domains.Raffles.Model.RaffleStatus;
import com.raffleease.raffleease.Domains.Raffles.Services.RafflesStatisticsService;
import com.raffleease.raffleease.Domains.Raffles.Services.RafflesStatusService;
import com.raffleease.raffleease.Domains.Tickets.Model.Ticket;
import com.raffleease.raffleease.Domains.Tickets.Services.TicketsQueryService;
import com.raffleease.raffleease.Domains.Tickets.Services.TicketsService;
import com.raffleease.raffleease.Domains.Notifications.Services.EmailsService;
import com.raffleease.raffleease.Common.Exceptions.CustomExceptions.BusinessException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

import static com.raffleease.raffleease.Domains.Orders.Model.OrderStatus.*;
import static com.raffleease.raffleease.Domains.Raffles.Model.RaffleStatus.ACTIVE;
import static com.raffleease.raffleease.Domains.Tickets.Model.TicketStatus.SOLD;

@RequiredArgsConstructor
@Service
public class OrdersEditServiceImpl implements OrdersEditService {
    private final OrdersPersistenceService ordersPersistenceService;
    private final TicketsService ticketsService;
    private final TicketsQueryService ticketsQueryService;
    private final OrdersMapper mapper;
    private final RafflesStatusService rafflesStatusService;
    private final RafflesStatisticsService statisticsService;
    private final EmailsService emailsService;

    @Override
    @Transactional
    public OrderDTO complete(Long orderId, OrderComplete orderComplete) {
        Order order = ordersPersistenceService.findById(orderId);
        throwIfInvalidOrderTransition(order.getStatus(), PENDING, COMPLETED);
        updateTicketsStatus(order);
        Raffle raffle = order.getRaffle();
        rafflesStatusService.completeRaffleIfAllTicketsSold(raffle);
        statisticsService.setCompleteStatistics(raffle, order.getOrderItems().stream().count());
        Payment payment = order.getPayment();
        payment.setPaymentMethod(orderComplete.paymentMethod());
        order.setStatus(COMPLETED);
        order.setCompletedAt(LocalDateTime.now());
        Order savedOrder = ordersPersistenceService.save(order);        
        emailsService.sendOrderCompletedEmail(savedOrder);
        return mapper.fromOrder(savedOrder);
    }

    @Override
    @Transactional
    public OrderDTO cancel(Long orderId) {
        Order order = ordersPersistenceService.findById(orderId);
        throwIfInvalidOrderTransition(order.getStatus(), PENDING, CANCELLED);
        throwIfInvalidRaffleTransition(order.getRaffle(), PENDING, CANCELLED, ACTIVE);
        releaseOrderTickets(order);
        statisticsService.setCancelStatistics(order.getRaffle(), order.getOrderItems().stream().count());
        order.setStatus(CANCELLED);
        order.setCancelledAt(LocalDateTime.now());
        Order savedOrder = ordersPersistenceService.save(order);
        emailsService.sendOrderCancelledEmail(savedOrder);        
        return mapper.fromOrder(savedOrder);
    }

    @Override
    @Transactional
    public OrderDTO refund(Long orderId) {
        Order order = ordersPersistenceService.findById(orderId);
        throwIfInvalidOrderTransition(order.getStatus(), COMPLETED, REFUNDED);
        releaseOrderTickets(order);
        Raffle raffle = order.getRaffle();
        statisticsService.setRefundStatistics(raffle, order.getOrderItems().stream().count());
        rafflesStatusService.updateStatusAfterAvailableTicketsIncrease(raffle);
        order.setStatus(REFUNDED);
        order.setRefundedAt(LocalDateTime.now());
        Order savedOrder = ordersPersistenceService.save(order);
        emailsService.sendOrderRefundedEmail(savedOrder);        
        return mapper.fromOrder(savedOrder);
    }

    @Override
    @Transactional
    public OrderDTO setUnpaid(Long orderId) {
        Order order = ordersPersistenceService.findById(orderId);
        throwIfInvalidOrderTransition(order.getStatus(), PENDING, UNPAID);
        throwIfInvalidRaffleTransition(order.getRaffle(), PENDING, UNPAID, RaffleStatus.COMPLETED);
        statisticsService.setUnpaidStatistics(order.getRaffle(), order.getOrderItems().stream().count());
        releaseOrderTickets(order);
        order.setStatus(UNPAID);
        order.setUnpaidAt(LocalDateTime.now());
        Order savedOrder = ordersPersistenceService.save(order);
        emailsService.sendOrderUnpaidEmail(savedOrder);        
        return mapper.fromOrder(savedOrder);
    }

    @Override
    public OrderDTO addComment(Long orderId, CommentRequest request) {
        Order order = ordersPersistenceService.findById(orderId);
        order.setComment(request.comment());
        return mapper.fromOrder(ordersPersistenceService.save(order));
    }

    @Override
    public void deleteComment(Long orderId) {
        Order order = ordersPersistenceService.findById(orderId);
        order.setComment(null);
        ordersPersistenceService.save(order);
    }

    private void throwIfInvalidOrderTransition(OrderStatus oldStatus, OrderStatus expectedStatus, OrderStatus newStatus) {
        if (!oldStatus.equals(expectedStatus)) {
            throw new BusinessException(String.format("Unsupported status transition from %s to %s", oldStatus, newStatus));
        }
    }

    private void throwIfInvalidRaffleTransition(Raffle raffle, OrderStatus oldStatus, OrderStatus newStatus, RaffleStatus expectedRaffleStatus) {
        if (!raffle.getStatus().equals(expectedRaffleStatus)) {
            throw new BusinessException(String.format("Cannot transition order from %s to %s when raffle status is %s, expected %s", 
                oldStatus, newStatus, raffle.getStatus(), expectedRaffleStatus));
        }
    }

    private void releaseOrderTickets(Order order) {
        List<Ticket> tickets = getTicketsFromOrder(order);
        ticketsService.releaseTickets(tickets);
    }

    private void updateTicketsStatus(Order order) {
        List<Ticket> tickets = getTicketsFromOrder(order);
        ticketsService.updateStatus(tickets, SOLD);
    }

    private List<Ticket> getTicketsFromOrder(Order order) {
        List<Long> ticketIds = order.getOrderItems().stream().map(OrderItem::getTicketId).toList();
        return ticketsQueryService.findAllById(ticketIds);
    }
}
