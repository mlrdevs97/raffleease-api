package com.raffleease.raffleease.Domains.Payments.Mappers.Impls;

import com.raffleease.raffleease.Domains.Payments.DTOs.PaymentDTO;
import com.raffleease.raffleease.Domains.Payments.Mappers.IPaymentsMapper;
import com.raffleease.raffleease.Domains.Payments.Model.Payment;
import org.springframework.stereotype.Service;

@Service
public class PaymentsMapper implements IPaymentsMapper {
    public PaymentDTO fromPayment(Payment payment) {
        String paymentMethod = payment.getPaymentMethod() != null ? payment.getPaymentMethod().getValue() : null;
        return PaymentDTO.builder()
                .paymentMethod(paymentMethod)
                .total(payment.getTotal())
                .createdAt(payment.getCreatedAt())
                .updatedAt(payment.getUpdatedAt())
                .build();
    }
}