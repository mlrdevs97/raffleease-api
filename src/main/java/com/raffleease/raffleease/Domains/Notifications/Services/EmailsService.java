package com.raffleease.raffleease.Domains.Notifications.Services;

import com.raffleease.raffleease.Domains.Orders.Model.Order;
import com.raffleease.raffleease.Domains.Users.Model.User;

public interface EmailsService {
    /**
     * Sends an email to the customer when an order is successfully created.
     * 
     * @param order the created order
     */
    void sendOrderCreatedEmail(Order order);

    /**
     * Sends an email to the customer when their order is completed.
     * 
     * @param order the completed order
     */
    void sendOrderCompletedEmail(Order order);

    /**
     * Sends an email to the customer when their order is cancelled.
     * 
     * @param order the cancelled order
     */
    void sendOrderCancelledEmail(Order order);

    /**
     * Sends an email to the customer when their order is refunded.
     * 
     * @param order the refunded order
     */
    void sendOrderRefundedEmail(Order order);

    /**
     * Sends an email to the customer when their order is set to unpaid.
     * 
     * @param order the unpaid order
     */
    void sendOrderUnpaidEmail(Order order);

    /**
     * Sends an email to the user to verify their email address.
     * 
     * @param user the user to send the email to
     * @param link the link to verify the email address
     */
    void sendEmailVerificationEmail(User user, String link);

    /**
     * Sends an email to the user to reset their password.
     * 
     * @param user the user to send the email to
     * @param link the link to reset the password
     */
    void sendPasswordResetEmail(User user, String link);

    /**
     * Sends an email to verify email update request.
     * 
     * @param user the user requesting the email update
     * @param newEmail the new email address to verify
     * @param link the verification link
     */
    void sendEmailUpdateVerificationEmail(User user, String newEmail, String link);

    /**
     * Sends an email to verify user creation by admin.
     * 
     * @param user the user to send the email to
     * @param associationName the name of the association
     * @param link the verification link
     */
    void sendUserCreationVerificationEmail(User user, String associationName, String link);
}
