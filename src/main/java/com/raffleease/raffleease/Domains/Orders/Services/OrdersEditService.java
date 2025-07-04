package com.raffleease.raffleease.Domains.Orders.Services;

import com.raffleease.raffleease.Domains.Orders.DTOs.*;

public interface OrdersEditService {
    /**
     * Completes a pending order.
     * An order can only be completed if it was previously in PENDING status, regardless of the raffle status being active or not.
     * A completed order can be refunded later, but not cancelled or set as unpaid.
     * After the order is completed, the tickets are marked as SOLD.
     * 
     * @param orderId the ID of the order to complete
     * @param orderComplete the order complete request
     * @return the completed order
     */
    OrderDTO complete(Long orderId, OrderComplete orderComplete);

    /**
     * Cancels a pending order.
     * An order can only be cancelled if it was previously in PENDING status.
     * An order can only be cancelled if the raffle is active.
     * A cancelled order cannot update its status later.
     * After the order is cancelled, the tickets are released.
     * 
     * @param orderId the ID of the order to cancel
     * @return the cancelled order
     */
    OrderDTO cancel(Long orderId);

    /**
     * Refunds a completed order.
     * An order can only be refunded if it was previously in COMPLETED status, regardless of the raffle status being active or completed.
     * A refunded order cannot update its status later.
     * Only ADMIN and MEMBER users can refund an order.
     * 
     * @param orderId the ID of the order to refund
     * @return the refunded order
     */
    OrderDTO refund(Long orderId);

    /**
     * Sets an order as unpaid.
     * An order can only be set as unpaid if it was previously in COMPLETED status.
     * An order can only be set as unpaid if the raffle is COMPLETED.
     * An unpaid order cannot update its status later.
     * Only ADMIN and MEMBER users can set an order as unpaid.
     * 
     * @param orderId the ID of the order to set as unpaid
     * @return the unpaid order
     */
    OrderDTO setUnpaid(Long orderId);

    /**
     * Adds a comment to an order.
     * An order can only have one comment.
     * 
     * @param orderId the ID of the order to add the comment to
     * @param request the comment request
     * @return the order with the comment
     */
    OrderDTO addComment(Long orderId, CommentRequest request);
    
    /**
     * Deletes a comment from an order.
     * 
     * @param orderId the ID of the order to delete the comment from
     */
    void deleteComment(Long orderId);
}
