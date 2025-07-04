package com.raffleease.raffleease.Common.Validations;

import com.raffleease.raffleease.Domains.Orders.DTOs.OrderSearchFilters;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.time.LocalDateTime;

public class ValidOrderSearchFiltersValidator implements ConstraintValidator<ValidOrderSearchFilters, OrderSearchFilters> {
    @Override
    public boolean isValid(OrderSearchFilters filters, ConstraintValidatorContext context) {
        boolean valid = true;
        context.disableDefaultConstraintViolation();

        if (filters.minTotal() != null && filters.maxTotal() != null &&
                filters.minTotal().compareTo(filters.maxTotal()) > 0) {
            context.buildConstraintViolationWithTemplate("Minimum total cannot be greater than maximum total")
                    .addPropertyNode("minTotal").addConstraintViolation();
            valid = false;
        }

        valid &= validateDateRange(filters.createdFrom(), filters.createdTo(), "createdFrom", "createdTo", context);
        valid &= validateDateRange(filters.completedFrom(), filters.completedTo(), "completedFrom", "completedTo", context);
        valid &= validateDateRange(filters.cancelledFrom(), filters.cancelledTo(), "cancelledFrom", "cancelledTo", context);

        return valid;
    }

    private boolean validateDateRange(LocalDateTime from, LocalDateTime to, String fromField, String toField, ConstraintValidatorContext context) {
        if (from != null && to != null && from.isAfter(to)) {
            context.buildConstraintViolationWithTemplate(fromField + " must be before or equal to " + toField)
                    .addPropertyNode(fromField)
                    .addConstraintViolation();
            return false;
        }
        return true;
    }
}