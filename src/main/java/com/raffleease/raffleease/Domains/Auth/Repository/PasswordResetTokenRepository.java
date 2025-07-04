package com.raffleease.raffleease.Domains.Auth.Repository;

import com.raffleease.raffleease.Domains.Auth.Model.PasswordResetToken;
import com.raffleease.raffleease.Domains.Users.Model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {
    Optional<PasswordResetToken> findByToken(String token);
    Optional<PasswordResetToken> findByUser(User user);
    void deleteByUser(User user);
} 