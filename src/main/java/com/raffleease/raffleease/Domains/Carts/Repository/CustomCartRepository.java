package com.raffleease.raffleease.Domains.Carts.Repository;

import com.raffleease.raffleease.Domains.Carts.Model.Cart;

import java.time.LocalDateTime;
import java.util.List;

public interface CustomCartRepository {
    void updateExpiredCart(LocalDateTime updatedAt);
    List<Cart> findExpiredCarts(LocalDateTime updatedAt);
}
