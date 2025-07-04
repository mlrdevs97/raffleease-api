package com.raffleease.raffleease.Domains.Auth.Repository;

import com.raffleease.raffleease.Domains.Auth.Model.VerificationToken;
import com.raffleease.raffleease.Domains.Users.Model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VerificationTokenRepository extends JpaRepository<VerificationToken, Long> {
    Optional<VerificationToken> findByToken(String token);
    Optional<VerificationToken> findByUser(User user);
    void deleteByUser(User user);
}