package com.raffleease.raffleease.Domains.Tickets.Repository.Impl;

import com.raffleease.raffleease.Domains.Raffles.Model.Raffle;
import com.raffleease.raffleease.Domains.Tickets.DTO.TicketsSearchFilters;
import com.raffleease.raffleease.Domains.Tickets.Model.Ticket;
import com.raffleease.raffleease.Domains.Tickets.Repository.TicketsSearchRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

@Repository
public class TicketsSearchRepositoryImpl implements TicketsSearchRepository {
    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Page<Ticket> search(TicketsSearchFilters searchFilters, Long associationId, Long raffleId, Pageable pageable) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Ticket> query = cb.createQuery(Ticket.class);
        Root<Ticket> root = query.from(Ticket.class);
        Join<Ticket, Raffle> raffleJoin = root.join("raffle");

        List<Predicate> predicates = buildPredicates(searchFilters, associationId, raffleId, cb, root, raffleJoin);
        query.select(root).where(predicates.toArray(new Predicate[0]));
        query.orderBy(cb.desc(root.get("createdAt")));

        List<Ticket> resultList = entityManager.createQuery(query)
                .setFirstResult((int) pageable.getOffset())
                .setMaxResults(pageable.getPageSize())
                .getResultList();

        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<Ticket> countRoot = countQuery.from(Ticket.class);
        Join<Ticket, Raffle> countRaffleJoin = countRoot.join("raffle");
        List<Predicate> countPredicates = buildPredicates(searchFilters, associationId, raffleId, cb, countRoot, countRaffleJoin);
        countQuery.select(cb.countDistinct(countRoot)).where(cb.and(countPredicates.toArray(new Predicate[0])));
        Long total = entityManager.createQuery(countQuery).getSingleResult();

        return new PageImpl<>(resultList, pageable, total);
    }

    private List<Predicate> buildPredicates(
            TicketsSearchFilters searchFilters,
            Long associationId,
            Long raffleId,
            CriteriaBuilder cb,
            Root<Ticket> root,
            Join<Ticket, Raffle> raffleJoin
    ) {
        List<Predicate> predicates = new ArrayList<>();

        predicates.add(cb.equal(root.get("raffle").get("id"), raffleId));
        predicates.add(cb.equal(raffleJoin.get("association").get("id"), associationId));

        if (searchFilters.status() != null) {
            predicates.add(cb.equal(root.get("status"), searchFilters.status()));
        }
        if (searchFilters.ticketNumber() != null) {
            predicates.add(cb.like(cb.lower(root.get("ticketNumber")), "%" + searchFilters.ticketNumber().toLowerCase() + "%"));
        }
        if (searchFilters.customerId() != null) {
            predicates.add(cb.equal(root.get("customer").get("id"), searchFilters.customerId()));
        }

        return predicates;
    }
}











