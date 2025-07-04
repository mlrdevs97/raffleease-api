package com.raffleease.raffleease.Domains.Auth.Services.Impl;

import com.raffleease.raffleease.Common.Configs.CorsProperties;
import com.raffleease.raffleease.Common.Exceptions.CustomExceptions.PasswordResetException;
import com.raffleease.raffleease.Domains.Auth.DTOs.ForgotPasswordRequest;
import com.raffleease.raffleease.Domains.Auth.DTOs.ResetPasswordRequest;
import com.raffleease.raffleease.Domains.Auth.Model.PasswordResetToken;
import com.raffleease.raffleease.Domains.Auth.Repository.PasswordResetTokenRepository;
import com.raffleease.raffleease.Domains.Auth.Services.PasswordResetService;
import com.raffleease.raffleease.Domains.Notifications.Services.EmailsService;
import com.raffleease.raffleease.Domains.Users.Model.User;
import com.raffleease.raffleease.Domains.Users.Services.UsersService;
import com.raffleease.raffleease.Common.Exceptions.CustomExceptions.DatabaseException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDateTime;
import java.util.UUID;

import static com.raffleease.raffleease.Common.Constants.Constants.EMAIL_VERIFICATION_EXPIRATION_MINUTES;

@Slf4j
@RequiredArgsConstructor
@Service
public class PasswordResetServiceImpl implements PasswordResetService {
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final UsersService usersService;
    private final EmailsService emailsService;
    private final PasswordEncoder passwordEncoder;
    private final CorsProperties corsProperties;

    @Transactional
    @Override
    public void requestPasswordReset(ForgotPasswordRequest request) {
        try {
            User user = usersService.getUserByEmail(request.email());

            PasswordResetToken passwordResetToken = passwordResetTokenRepository.findByUser(user)
                    .map(existingToken -> {
                        existingToken.setToken(UUID.randomUUID().toString());
                        existingToken.setExpiryDate(LocalDateTime.now().plusMinutes(EMAIL_VERIFICATION_EXPIRATION_MINUTES));
                        return passwordResetTokenRepository.save(existingToken);
                    })
                    .orElseGet(() -> createPasswordResetToken(user));
            
            String resetLink = UriComponentsBuilder.fromHttpUrl(corsProperties.getClientAsList().get(0))
                    .path("/auth/reset-password")
                    .queryParam("token", passwordResetToken.getToken())
                    .build()
                    .toUriString();
            emailsService.sendPasswordResetEmail(user, resetLink);
            
            log.info("Password reset email sent successfully to: {}", request.email());
        } catch (Exception ex) {
            log.error("Error processing password reset request for email: {}", request.email(), ex);
        }
    }

    @Transactional
    @Override
    public void resetPassword(ResetPasswordRequest request) {
        PasswordResetToken passwordResetToken = passwordResetTokenRepository.findByToken(request.token())
                .orElseThrow(() -> new PasswordResetException("Invalid or expired password reset token"));

        if (passwordResetToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            passwordResetTokenRepository.delete(passwordResetToken);
            throw new PasswordResetException("Password reset token has expired");
        }

        String encodedPassword = passwordEncoder.encode(request.password());
        usersService.updatePassword(passwordResetToken.getUser(), encodedPassword);
        passwordResetTokenRepository.delete(passwordResetToken);
        log.info("Password reset completed successfully for user: {}", passwordResetToken.getUser().getUserName());
    }

    private PasswordResetToken createPasswordResetToken(User user) {
        try {
            return passwordResetTokenRepository.save(PasswordResetToken.builder()
                    .token(UUID.randomUUID().toString())
                    .user(user)
                    .expiryDate(LocalDateTime.now().plusMinutes(EMAIL_VERIFICATION_EXPIRATION_MINUTES))
                    .build());
        } catch (DataAccessException ex) {
            log.info(ex.toString());
            throw new DatabaseException("Database error occurred while saving password reset token");
        }
    }
} 