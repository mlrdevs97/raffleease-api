package com.raffleease.raffleease.Domains.Users.Services.Impls;

import com.raffleease.raffleease.Common.Configs.CorsProperties;
import com.raffleease.raffleease.Domains.Auth.Model.EmailUpdateToken;
import com.raffleease.raffleease.Domains.Auth.Repository.EmailUpdateTokenRepository;
import com.raffleease.raffleease.Domains.Notifications.Services.EmailsService;
import com.raffleease.raffleease.Domains.Users.DTOs.UpdateEmailRequest;
import com.raffleease.raffleease.Domains.Users.DTOs.VerifyEmailUpdateRequest;
import com.raffleease.raffleease.Domains.Users.Model.User;
import com.raffleease.raffleease.Domains.Users.Services.EmailUpdateService;
import com.raffleease.raffleease.Domains.Users.Services.UsersService;
import com.raffleease.raffleease.Common.Exceptions.CustomExceptions.DatabaseException;
import com.raffleease.raffleease.Common.Exceptions.CustomExceptions.EmailVerificationException;
import com.raffleease.raffleease.Common.Exceptions.CustomExceptions.BusinessException;
import com.raffleease.raffleease.Common.Exceptions.CustomExceptions.UniqueConstraintViolationException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDateTime;
import java.util.UUID;

import static com.raffleease.raffleease.Common.Constants.Constants.EMAIL_VERIFICATION_EXPIRATION_MINUTES;
import static com.raffleease.raffleease.Common.Exceptions.ErrorCodes.EMAIL_SAME_AS_CURRENT;
import static com.raffleease.raffleease.Common.Exceptions.ErrorCodes.EMAIL_UPDATE_TOKEN_EXPIRED;
import static com.raffleease.raffleease.Common.Exceptions.ErrorCodes.EMAIL_UPDATE_TOKEN_INVALID;
import static com.raffleease.raffleease.Common.Exceptions.ErrorCodes.EMAIL_NO_LONGER_AVAILABLE;

@Slf4j
@RequiredArgsConstructor
@Service
public class EmailUpdateServiceImpl implements EmailUpdateService {
    private final EmailUpdateTokenRepository emailUpdateTokenRepository;
    private final UsersService usersService;
    private final EmailsService emailsService;
    private final CorsProperties corsProperties;

    @Transactional
    @Override
    public void requestEmailUpdate(Long userId, UpdateEmailRequest request) {
        try {
            User user = usersService.findById(userId);
            
            if (user.getEmail().equals(request.newEmail())) {
                throw new BusinessException("The new email must be different from your current email", EMAIL_SAME_AS_CURRENT);
            }            
            if (usersService.existsByEmail(request.newEmail())) {
                throw new UniqueConstraintViolationException("email", "This email address is already in use");
            }
            
            EmailUpdateToken emailUpdateToken = emailUpdateTokenRepository.findByUser(user)
                    .map(existingToken -> {
                        existingToken.setToken(UUID.randomUUID().toString());
                        existingToken.setNewEmail(request.newEmail());
                        existingToken.setExpiryDate(LocalDateTime.now().plusMinutes(EMAIL_VERIFICATION_EXPIRATION_MINUTES));
                        return emailUpdateTokenRepository.save(existingToken);
                    })
                    .orElseGet(() -> createEmailUpdateToken(user, request.newEmail()));
            
            String verificationLink = UriComponentsBuilder.fromHttpUrl(corsProperties.getClientAsList().get(0))
                    .path("/profile/verify-email-update")
                    .queryParam("token", emailUpdateToken.getToken())
                    .build()
                    .toUriString();
            emailsService.sendEmailUpdateVerificationEmail(user, request.newEmail(), verificationLink);
            
            log.info("Email update verification email sent successfully to: {} for user: {}", 
                    request.newEmail(), user.getUserName());
        } catch (Exception ex) {
            log.error("Error processing email update request for user ID: {}", userId, ex);
            throw ex;
        }
    }

    @Transactional
    @Override
    public void verifyEmailUpdate(VerifyEmailUpdateRequest request) {
        EmailUpdateToken emailUpdateToken = emailUpdateTokenRepository.findByToken(request.token())
                .orElseThrow(() -> new EmailVerificationException("Invalid or expired email update token", EMAIL_UPDATE_TOKEN_INVALID));

        if (emailUpdateToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            emailUpdateTokenRepository.delete(emailUpdateToken);
            throw new EmailVerificationException("Email update token has expired", EMAIL_UPDATE_TOKEN_EXPIRED);
        }

        if (usersService.existsByEmail(emailUpdateToken.getNewEmail())) {
            emailUpdateTokenRepository.delete(emailUpdateToken);
            throw new BusinessException("This email address is no longer available", EMAIL_NO_LONGER_AVAILABLE);
        }

        User user = emailUpdateToken.getUser();
        user.setEmail(emailUpdateToken.getNewEmail());
        usersService.save(user);
        emailUpdateTokenRepository.delete(emailUpdateToken);
        
        log.info("Email update completed successfully for user: {} to new email: {}", 
                user.getUserName(), user.getEmail());
    }

    private EmailUpdateToken createEmailUpdateToken(User user, String newEmail) {
        try {
            return emailUpdateTokenRepository.save(EmailUpdateToken.builder()
                    .token(UUID.randomUUID().toString())
                    .user(user)
                    .newEmail(newEmail)
                    .expiryDate(LocalDateTime.now().plusMinutes(EMAIL_VERIFICATION_EXPIRATION_MINUTES))
                    .build());
        } catch (DataAccessException ex) {
            log.error("Database error occurred while saving email update token", ex);
            throw new DatabaseException("Database error occurred while saving email update token");
        }
    }
} 