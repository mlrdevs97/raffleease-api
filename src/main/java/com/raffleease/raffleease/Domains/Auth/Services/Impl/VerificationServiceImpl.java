package com.raffleease.raffleease.Domains.Auth.Services.Impl;

import com.raffleease.raffleease.Domains.Auth.Model.VerificationToken;
import com.raffleease.raffleease.Domains.Auth.Repository.VerificationTokenRepository;
import com.raffleease.raffleease.Domains.Auth.Services.VerificationService;
import com.raffleease.raffleease.Domains.Users.Services.UsersService;
import com.raffleease.raffleease.Common.Exceptions.CustomExceptions.EmailVerificationException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@RequiredArgsConstructor
@Service
public class VerificationServiceImpl implements VerificationService {
    private final VerificationTokenRepository repository;
    private final UsersService usersService;

    @Override
    public void verifyEmail(String token) {
        VerificationToken verificationToken = repository.findByToken(token).orElseThrow(
                () -> new EmailVerificationException("Verification token not found")
        );

        if (verificationToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            throw new EmailVerificationException("Verification token is expired");
        }

        usersService.setUserEnabled(verificationToken.getUser(), true);
        repository.delete(verificationToken);
    }
}
