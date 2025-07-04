package com.raffleease.raffleease.Domains.Orders.Repository.Impl;

import com.raffleease.raffleease.Domains.Customers.Model.Customer;
import com.raffleease.raffleease.Domains.Customers.Model.CustomersPhoneNumber;
import com.raffleease.raffleease.Domains.Orders.DTOs.OrderSearchFilters;
import com.raffleease.raffleease.Domains.Orders.Model.Order;
import com.raffleease.raffleease.Domains.Orders.Model.OrderItem;
import com.raffleease.raffleease.Domains.Orders.Repository.OrdersSearchRepository;
import com.raffleease.raffleease.Domains.Payments.Model.Payment;
import com.raffleease.raffleease.Domains.Raffles.Model.Raffle;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static jakarta.persistence.criteria.JoinType.LEFT;

@Repository
@RequiredArgsConstructor
public class OrdersSearchRepositoryImpl implements OrdersSearchRepository {
    private final EntityManager entityManager;

    @Override
    public Page<Order> searchOrders(OrderSearchFilters filters, Long associationId, Pageable pageable) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Order> query = cb.createQuery(Order.class);
        Root<Order> root = query.from(Order.class);
        Join<Order, Raffle> raffleJoin = root.join("raffle");
        Join<Order, Customer> customerJoin = root.join("customer", LEFT);
        Join<Order, Payment> paymentJoin = root.join("payment");
        Join<Order, OrderItem> itemJoin = root.join("orderItems", LEFT);

        List<Predicate> predicates = buildPredicates(filters, associationId, cb, root, raffleJoin, customerJoin, paymentJoin, itemJoin);
        query.where(cb.and(predicates.toArray(new Predicate[0])));
        query.distinct(true);
        query.orderBy(cb.desc(root.get("createdAt")));

        List<Order> resultList = entityManager.createQuery(query)
                .setFirstResult((int) pageable.getOffset())
                .setMaxResults(pageable.getPageSize())
                .getResultList();

        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<Order> countRoot = countQuery.from(Order.class);
        Join<Order, Raffle> countRaffleJoin = countRoot.join("raffle");
        Join<Order, Customer> countCustomerJoin = countRoot.join("customer", LEFT);
        Join<Order, Payment> countPaymentJoin = countRoot.join("payment");
        Join<Order, OrderItem> countItemJoin = countRoot.join("orderItems", LEFT);
        List<Predicate> countPredicates = buildPredicates(filters, associationId, cb, countRoot, countRaffleJoin, countCustomerJoin, countPaymentJoin, countItemJoin);
        countQuery.select(cb.countDistinct(countRoot));
        countQuery.where(cb.and(countPredicates.toArray(new Predicate[0])));
        Long total = entityManager.createQuery(countQuery).getSingleResult();

        return new PageImpl<>(resultList, pageable, total);
    }

    private void applyDateRange(
            CriteriaBuilder cb,
            List<Predicate> predicates,
            Path<LocalDateTime> path,
            LocalDateTime from,
            LocalDateTime to
    ) {
        if (from != null) predicates.add(cb.greaterThanOrEqualTo(path, from));
        if (to != null) predicates.add(cb.lessThanOrEqualTo(path, to));
    }

    private List<Predicate> buildPredicates(
            OrderSearchFilters filters,
            Long associationId,
            CriteriaBuilder cb,
            Root<Order> root,
            Join<Order, Raffle> raffleJoin,
            Join<Order, Customer> customerJoin,
            Join<Order, Payment> paymentJoin,
            Join<Order, OrderItem> itemJoin
    ) {
        List<Predicate> predicates = new ArrayList<>();

        predicates.add(cb.equal(raffleJoin.get("association").get("id"), associationId));

        if (filters.status() != null) {
            predicates.add(cb.equal(root.get("status"), filters.status()));
        }

        if (filters.paymentMethod() != null) {
            predicates.add(cb.equal(paymentJoin.get("paymentMethod"), filters.paymentMethod()));
        }

        if (filters.orderReference() != null && !filters.orderReference().isBlank()) {
            predicates.add(cb.like(cb.lower(root.get("orderReference")), "%" + filters.orderReference().toLowerCase() + "%"));
        }

        if (filters.customerName() != null && !filters.customerName().isBlank()) {
            predicates.add(cb.like(cb.lower(customerJoin.get("fullName")), "%" + filters.customerName().toLowerCase() + "%"));
        }

        if (filters.customerEmail() != null && !filters.customerEmail().isBlank()) {
            predicates.add(cb.like(cb.lower(customerJoin.get("email")), "%" + filters.customerEmail().toLowerCase() + "%"));
        }

        if (filters.customerPhone() != null && !filters.customerPhone().isBlank()) {
            Join<Customer, CustomersPhoneNumber> phoneJoin = customerJoin.join("phoneNumber", LEFT);
            Expression<String> concatenatedPhone = cb.concat(phoneJoin.get("prefix"), phoneJoin.get("nationalNumber"));
            predicates.add(cb.like(concatenatedPhone, "%" + filters.customerPhone() + "%"));
        }

        if (filters.raffleId() != null) {
            predicates.add(cb.equal(itemJoin.get("raffleId"), filters.raffleId()));
        }

        if (filters.minTotal() != null) {
            predicates.add(cb.ge(paymentJoin.get("total"), filters.minTotal()));
        }

        if (filters.maxTotal() != null) {
            predicates.add(cb.le(paymentJoin.get("total"), filters.maxTotal()));
        }

        applyDateRange(cb, predicates, root.get("createdAt"), filters.createdFrom(), filters.createdTo());
        applyDateRange(cb, predicates, root.get("completedAt"), filters.completedFrom(), filters.completedTo());
        applyDateRange(cb, predicates, root.get("cancelledAt"), filters.cancelledFrom(), filters.cancelledTo());

        return predicates;
    }
}