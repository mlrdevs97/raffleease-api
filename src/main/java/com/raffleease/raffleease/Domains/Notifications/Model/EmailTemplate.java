package com.raffleease.raffleease.Domains.Notifications.Model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum EmailTemplate {
    ORDER_CREATED("order-created.html", "Order created successfully"),
    ORDER_COMPLETED("order-completed.html", "Your order has been completed"),
    ORDER_CANCELLED("order-cancelled.html", "Your order has been cancelled"),
    ORDER_REFUNDED("order-refunded.html", "Your order has been refunded"),
    ORDER_UNPAID("order-unpaid.html", "Payment required for your order"),
    EMAIL_VERIFICATION("email-verification.html", "Verify your email"),
    PASSWORD_RESET("password-reset.html", "Reset your password"),
    EMAIL_UPDATE_VERIFICATION("email-update-verification.html", "Verify your new email address"),
    USER_CREATION_VERIFICATION("user-creation-verification.html", "Verify your account");

    private final String template;
    private final String subject;
}