package com.raffleease.raffleease.Domains.Carts.Repository;

import com.raffleease.raffleease.Domains.Carts.Model.Cart;
import com.raffleease.raffleease.Domains.Carts.Model.CartStatus;
import com.raffleease.raffleease.Domains.Users.Model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CartsRepository extends JpaRepository<Cart, Long> {
    List<Cart> findAllByUpdatedAtBefore(LocalDateTime updatedAt);
    Optional<Cart> findByUserAndStatus(User user, CartStatus status);
}
