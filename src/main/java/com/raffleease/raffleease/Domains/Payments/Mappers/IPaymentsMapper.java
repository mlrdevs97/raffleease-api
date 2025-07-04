package com.raffleease.raffleease.Domains.Payments.Mappers;

import com.raffleease.raffleease.Domains.Payments.DTOs.PaymentDTO;
import com.raffleease.raffleease.Domains.Payments.Model.Payment;

public interface IPaymentsMapper {
    PaymentDTO fromPayment(Payment payment);
}
