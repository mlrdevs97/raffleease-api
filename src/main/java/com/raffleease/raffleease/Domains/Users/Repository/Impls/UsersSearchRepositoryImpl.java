package com.raffleease.raffleease.Domains.Users.Repository.Impls;

import com.raffleease.raffleease.Domains.Associations.Model.Association;
import com.raffleease.raffleease.Domains.Associations.Model.AssociationMembership;
import com.raffleease.raffleease.Domains.Users.DTOs.UserSearchFilters;
import com.raffleease.raffleease.Domains.Users.Model.User;
import com.raffleease.raffleease.Domains.Users.Model.UserPhoneNumber;
import com.raffleease.raffleease.Domains.Users.Repository.UsersSearchRepository;
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
public class UsersSearchRepositoryImpl implements UsersSearchRepository {
    private final EntityManager entityManager;

    @Override
    public Page<User> search(UserSearchFilters searchFilters, Long associationId, Pageable pageable) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<User> query = cb.createQuery(User.class);

        Root<AssociationMembership> membership = query.from(AssociationMembership.class);
        Join<AssociationMembership, User> userJoin = membership.join("user");
        Join<AssociationMembership, Association> associationJoin = membership.join("association");
        
        List<Predicate> predicates = buildPredicates(searchFilters, associationId, cb, userJoin, associationJoin, membership);
        query.select(userJoin).distinct(true).where(predicates.toArray(new Predicate[0]));
        query.orderBy(cb.desc(userJoin.get("createdAt")));
        
        List<User> resultList = entityManager.createQuery(query)
                .setFirstResult((int) pageable.getOffset())
                .setMaxResults(pageable.getPageSize())
                .getResultList();

        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<AssociationMembership> countMembership = countQuery.from(AssociationMembership.class);
        Join<AssociationMembership, User> countUserJoin = countMembership.join("user");
        Join<AssociationMembership, Association> countAssociationJoin = countMembership.join("association");
        
        List<Predicate> countPredicates = buildPredicates(searchFilters, associationId, cb, countUserJoin, countAssociationJoin, countMembership);
        countQuery.select(cb.countDistinct(countUserJoin)).where(cb.and(countPredicates.toArray(new Predicate[0])));
        Long total = entityManager.createQuery(countQuery).getSingleResult();

        return new PageImpl<>(resultList, pageable, total);
    }

    private List<Predicate> buildPredicates(
            UserSearchFilters searchFilters,
            Long associationId,
            CriteriaBuilder cb,
            Join<AssociationMembership, User> userJoin,
            Join<AssociationMembership, Association> associationJoin,
            Root<AssociationMembership> membership
    ) {
        List<Predicate> predicates = new ArrayList<>();

        predicates.add(cb.equal(associationJoin.get("id"), associationId));

        if (searchFilters.fullName() != null && !searchFilters.fullName().isBlank()) {
            Expression<String> fullName = cb.concat(
                cb.concat(userJoin.get("firstName"), " "), 
                userJoin.get("lastName")
            );
            predicates.add(cb.like(cb.lower(fullName), "%" + searchFilters.fullName().toLowerCase() + "%"));
        }
        
        if (searchFilters.email() != null && !searchFilters.email().isBlank()) {
            predicates.add(cb.like(cb.lower(userJoin.get("email")), "%" + searchFilters.email().toLowerCase() + "%"));
        }
        
        if (searchFilters.phoneNumber() != null && !searchFilters.phoneNumber().isBlank()) {
            Join<User, UserPhoneNumber> phoneJoin = userJoin.join("phoneNumber", LEFT);
            Expression<String> concatenatedPhone = cb.concat(phoneJoin.get("prefix"), phoneJoin.get("nationalNumber"));
            predicates.add(cb.like(concatenatedPhone, "%" + searchFilters.phoneNumber() + "%"));
        }
        
        if (searchFilters.role() != null) {
            predicates.add(cb.equal(membership.get("role"), searchFilters.role()));
        }

        return predicates;
    }
} 