package com.raffleease.raffleease.Domains.Payments.Model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum PaymentMethod {
    // --- Card payments ---
    CARD("card"),
    VISA("visa"),
    MASTERCARD("mastercard"),
    AMERICAN_EXPRESS("amex"),

    // --- Wallets ---
    ALIPAY("alipay"),
    AMAZON_PAY("amazon_pay"),
    APPLE_PAY("apple_pay"),
    GOOGLE_PAY("google_pay"),
    LINK("link"),
    PAYPAL("paypal"),
    WECHAT_PAY("wechat_pay"),

    // --- Buy now, pay later (BNPL) ---
    KLARNA("klarna"),

    // --- Manual/admin-created ---
    BIZUM("bizum"),
    BANK_TRANSFER("bank_transfer"),
    CASH("cash"),

    // --- Fallback ---
    UNKNOWN("unknown");

    private final String value;

    PaymentMethod(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static PaymentMethod fromValue(String value) {
        for (PaymentMethod method : values()) {
            if (method.value.equalsIgnoreCase(value)) {
                return method;
            }
        }
        return UNKNOWN;
    }
}