package com.raffleease.raffleease.Domains.Carts.Repository.Impl;

import com.raffleease.raffleease.Domains.Carts.Model.Cart;
import com.raffleease.raffleease.Domains.Carts.Repository.CustomCartRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

import static com.raffleease.raffleease.Domains.Carts.Model.CartStatus.ACTIVE;
import static com.raffleease.raffleease.Domains.Carts.Model.CartStatus.EXPIRED;

@Repository
public class CustomCartRepositoryImpl implements CustomCartRepository {
    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public void updateExpiredCart(LocalDateTime updatedAt) {
        String query = "UPDATE Cart c " +
                "SET c.status = :expired, " +
                "c.updatedAt = :currentTimestamp " +
                "WHERE c.updatedAt < :updatedAt " +
                "AND c.status = :active";

        entityManager.createQuery(query)
                .setParameter("updatedAt", updatedAt)
                .setParameter("currentTimestamp", LocalDateTime.now())
                .setParameter("expired", EXPIRED)
                .setParameter("active", ACTIVE)
                .executeUpdate();
    }


    @Override
    public List<Cart> findExpiredCarts(LocalDateTime updatedAt) {
        String query = "SELECT c FROM Cart c " +
                "WHERE c.updatedAt < :updatedAt " +
                "AND c.status = :status";

        return entityManager.createQuery(query, Cart.class)
                .setParameter("updatedAt", updatedAt)
                .setParameter("status", ACTIVE)
                .getResultList();
    }
}
