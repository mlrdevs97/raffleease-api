package com.raffleease.raffleease.Domains.Customers.Repository.Impls;

import com.raffleease.raffleease.Domains.Associations.Model.Association;
import com.raffleease.raffleease.Domains.Customers.DTO.CustomerSearchFilters;
import com.raffleease.raffleease.Domains.Customers.Model.Customer;
import com.raffleease.raffleease.Domains.Customers.Model.CustomersPhoneNumber;
import com.raffleease.raffleease.Domains.Customers.Repository.CustomersSearchRepository;
import com.raffleease.raffleease.Domains.Raffles.Model.Raffle;
import com.raffleease.raffleease.Domains.Tickets.Model.Ticket;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

import static jakarta.persistence.criteria.JoinType.LEFT;

@RequiredArgsConstructor
@Repository
public class CustomersSearchRepositoryImpl implements CustomersSearchRepository {
    private final EntityManager entityManager;

    @Override
    public Page<Customer> search(CustomerSearchFilters searchFilters, Long associationId, Pageable pageable) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Customer> query = cb.createQuery(Customer.class);

        Root<Ticket> ticket = query.from(Ticket.class);
        Join<Ticket, Customer> customerJoin = ticket.join("customer");
        Join<Ticket, Raffle> raffleJoin = ticket.join("raffle");
        Join<Raffle, Association> associationJoin = raffleJoin.join("association");
        List<Predicate> predicates = buildPredicates(searchFilters, associationId, cb, customerJoin, associationJoin);
        query.select(customerJoin).distinct(true).where(predicates.toArray(new Predicate[0]));
        query.orderBy(cb.desc(customerJoin.get("createdAt")));
        List<Customer> resultList = entityManager.createQuery(query)
                .setFirstResult((int) pageable.getOffset())
                .setMaxResults(pageable.getPageSize())
                .getResultList();

        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<Ticket> countTicket = countQuery.from(Ticket.class);
        Join<Ticket, Customer> countCustomerJoin = countTicket.join("customer");
        Join<Ticket, Raffle> countRaffleJoin = countTicket.join("raffle");
        Join<Raffle, Association> countAssociationJoin = countRaffleJoin.join("association");
        List<Predicate> countPredicates = buildPredicates(searchFilters, associationId, cb, countCustomerJoin, countAssociationJoin);
        countQuery.select(cb.countDistinct(countCustomerJoin)).where(cb.and(countPredicates.toArray(new Predicate[0])));
        Long total = entityManager.createQuery(countQuery).getSingleResult();

        return new PageImpl<>(resultList, pageable, total);
    }

    private List<Predicate> buildPredicates(
            CustomerSearchFilters searchFilters,
            Long associationId,
            CriteriaBuilder cb,
            Join<Ticket, Customer> customerJoin,
            Join<Raffle, Association> associationJoin
    ) {
        List<Predicate> predicates = new ArrayList<>();

        predicates.add(cb.equal(associationJoin.get("id"), associationId));

        if (searchFilters.fullName() != null && !searchFilters.fullName().isBlank()) {
            predicates.add(cb.like(cb.lower(customerJoin.get("fullName")), "%" + searchFilters.fullName().toLowerCase() + "%"));
        }
        if (searchFilters.email() != null && !searchFilters.email().isBlank()) {
            predicates.add(cb.like(cb.lower(customerJoin.get("email")), "%" + searchFilters.email().toLowerCase() + "%"));
        }
        if (searchFilters.phoneNumber() != null && !searchFilters.phoneNumber().isBlank()) {
            Join<Customer, CustomersPhoneNumber> phoneJoin = customerJoin.join("phoneNumber", LEFT);
            Expression<String> concatenatedPhone = cb.concat(phoneJoin.get("prefix"), phoneJoin.get("nationalNumber"));
            predicates.add(cb.like(concatenatedPhone, "%" + searchFilters.phoneNumber() + "%"));
        }

        return predicates;
    }
}
