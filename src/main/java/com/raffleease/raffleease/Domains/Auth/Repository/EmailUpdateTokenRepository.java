package com.raffleease.raffleease.Domains.Auth.Repository;

import com.raffleease.raffleease.Domains.Auth.Model.EmailUpdateToken;
import com.raffleease.raffleease.Domains.Users.Model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EmailUpdateTokenRepository extends JpaRepository<EmailUpdateToken, Long> {
    Optional<EmailUpdateToken> findByToken(String token);
    Optional<EmailUpdateToken> findByUser(User user);
    void deleteByUser(User user);
} 