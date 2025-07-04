package com.raffleease.raffleease.Domains.Raffles.Services;

import com.raffleease.raffleease.Domains.Raffles.Model.Raffle;
import com.raffleease.raffleease.Domains.Tickets.Model.Ticket;

import java.util.List;

public interface RafflesStatisticsService {

    /**
     * Updates raffle statistics when tickets are reserved.
     * 
     * This method is called when an user reserves tickets.
     * It decreases the available ticket count, increases the participant count, and
     * recalculates the tickets-per-participant ratio.
     * 
     * Business rules:
     * - Available tickets cannot go below zero
     * - Participant count is incremented by 1
     * - Tickets-per-participant ratio is recalculated based on unavailable tickets
     * 
     * @param raffle the raffle whose statistics need to be updated
     * @param reductionQuantity the number of tickets being reserved
     * @throws com.raffleease.raffleease.Common.Exceptions.CustomExceptions.BusinessException 
     *         if insufficient tickets are available to complete the operation
     */
    void setReservationStatistics(Raffle raffle, long reductionQuantity);

    /**
     * Updates raffle statistics when previously reserved tickets are released.
     * 
     * This method is called when a ticket reservation is cancelled, when an order is cancelled before completion or when an order is set as unpaid.
     * It increases the available ticket count, decreases the participant count, and recalculates the tickets-per-participant ratio.
     * 
     * Business rules:
     * - Available tickets cannot exceed the total ticket limit
     * - Participant count is decremented by 1
     * - Tickets-per-participant ratio is recalculated
     * 
     * @param raffle the raffle whose statistics need to be updated
     * @param increaseQuantity the number of tickets being released back (must be positive)
     * @throws com.raffleease.raffleease.Common.Exceptions.CustomExceptions.BusinessException
     *         if the operation would exceed the total ticket limit
     */
    void setReleaseStatistics(Raffle raffle, long increaseQuantity);

    /**
     * Updates raffle statistics when a new order is created.
     * 
     * This method is called when an order is created.
     * It increments the pending order count and the total order count.
     * 
     * @param raffle the raffle whose statistics need to be updated
     * @param reservedTickets the number of tickets in the created order
     */
    void setCreateOrderStatistics(Raffle raffle, long reservedTickets);

    /**
     * Updates raffle statistics when an order is successfully completed.
     * 
     * This method is called when an order is completed.
     * It decrements the pending order count and increments the completed order count.
     * It also increments the sold tickets count and adds to the total revenue.
     * It also recalculates the average order value and sets the first sale date and the last sale date.
     * 
     * Updated statistics:
     * - Decrements pending orders, increments completed orders
     * - Increases sold tickets count
     * - Adds to total revenue based on ticket price × quantity
     * - Recalculates average order value
     * - Sets first sale date (if this is the first sale)
     * - Updates last sale date to current timestamp
     * 
     * @param raffle the raffle whose statistics need to be updated
     * @param soldTickets the number of tickets sold in the completed order
     */
    void setCompleteStatistics(Raffle raffle, long soldTickets);

    /**
     * Updates raffle statistics when a completed order is refunded.
     * 
     * This method is called when a completed order is refunded.
     * It increments the refunded order count and decrements the completed order count.
     * It also decrements the sold tickets count and subtracts from the total revenue.
     * It also recalculates the average order value.
     * It also releases the tickets back to the available pool.
     * 
     * Updated statistics:
     * - Increments refunded orders, decrements completed orders
     * - Decreases sold tickets count and increases available tickets
     * - Subtracts refund amount from total revenue
     * - Recalculates average order value (handles division by zero)
     * - Returns tickets to the available pool
     * 
     * @param raffle the raffle whose statistics need to be updated
     * @param refundTickets the number of tickets being refunded (should match original order)
     */
    void setRefundStatistics(Raffle raffle, long refundTickets);

    /**
     * Updates raffle statistics when an order is cancelled before completion.
     * 
     * This method is called when an order is cancelled before completion.
     * It decrements the pending order count and increments the cancelled order count.
     * It also releases the tickets back to the available pool.
     * 
     * Updated statistics:
     * - Decrements pending orders, increments cancelled orders
     * - Increases available tickets (releases reserved tickets)
     * - Maintains order volume tracking for analytics
     * 
     * @param raffle the raffle whose statistics need to be updated
     * @param cancelledTickets the number of tickets in the cancelled order
     */
    void setCancelStatistics(Raffle raffle, long cancelledTickets);

    /**
     * Updates raffle statistics when an order becomes unpaid (payment timeout or failure).
     * 
     * This method is called when an order is set as unpaid.
     * It decrements the pending order count and increments the unpaid order count.
     * It also releases the tickets back to the available pool.
     * 
     * Updated statistics:
     * - Decrements pending orders, increments unpaid orders
     * - Increases available tickets (releases reserved tickets)
     * - Maintains order pipeline visibility
     * 
     * @param raffle the raffle whose statistics need to be updated
     * @param unpaidTickets the number of tickets in the unpaid order
     */
    void setUnpaidStatistics(Raffle raffle, long unpaidTickets);

    /**
     * Bulk operation to increase ticket availability across multiple raffles.
     * 
     * This method processes a list of tickets and groups them by raffle,
     * then calls setReleaseStatistics for each raffle with the appropriate
     * ticket count. It's used for batch operations where multiple tickets
     * across different raffles need to be released simultaneously.
     * 
     * Use cases:
     * - Bulk order cancellations
     * - System-wide ticket releases
     * - Batch refund processing
     * 
     * @param tickets the list of tickets to be released (grouped by raffle internally)
     */
    void increaseRafflesTicketsAvailability(List<Ticket> tickets);

    /**
     * Bulk operation to decrease ticket availability across multiple raffles.
     * 
     * This method processes a list of tickets and groups them by raffle,
     * then calls setReservationStatistics for each raffle with the appropriate
     * ticket count.
     * 
     * Use cases:
     * - Bulk ticket reservations
     * - System-wide ticket allocations
     * - Batch order processing
     * 
     * @param tickets the list of tickets to be reserved (grouped by raffle internally)
     */
    void reduceRaffleTicketsAvailability(List<Ticket> tickets);
}
