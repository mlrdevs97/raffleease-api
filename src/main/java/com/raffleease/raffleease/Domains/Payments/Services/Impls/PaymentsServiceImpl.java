package com.raffleease.raffleease.Domains.Payments.Services.Impls;

import com.raffleease.raffleease.Domains.Orders.Model.Order;
import com.raffleease.raffleease.Domains.Payments.Model.Payment;
import com.raffleease.raffleease.Domains.Payments.Repository.PaymentsRepository;
import com.raffleease.raffleease.Domains.Payments.Services.PaymentsService;
import com.raffleease.raffleease.Common.Exceptions.CustomExceptions.DatabaseException;
import com.raffleease.raffleease.Common.Exceptions.CustomExceptions.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@RequiredArgsConstructor
@Service
public class PaymentsServiceImpl implements PaymentsService {
    private final PaymentsRepository repository;

    @Override
    public Payment create(Order order, BigDecimal total) {
        return save(Payment.builder()
        .order(order)
        .total(total)
        .build());
    }

    public Payment findById(Long id) {
        try {
            return repository.findById(id).orElseThrow(() -> new NotFoundException("Payment not found for id <" + id + ">"));
        } catch (DataAccessException ex) {
            throw new DatabaseException("Database error occurred while fetching payment with ID <" + id + ">: " + ex.getMessage());
        }
    }

    private Payment save(Payment payment) {
        try {
            return repository.save(payment);
        } catch (DataAccessException ex) {
            throw new DatabaseException("Database error occurred while saving payment: " + ex.getMessage());
        }
    }
}