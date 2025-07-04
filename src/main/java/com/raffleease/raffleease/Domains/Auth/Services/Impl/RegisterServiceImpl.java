package com.raffleease.raffleease.Domains.Auth.Services.Impl;

import com.raffleease.raffleease.Common.Configs.CorsProperties;
import com.raffleease.raffleease.Domains.Associations.Model.Association;
import com.raffleease.raffleease.Domains.Associations.Services.AssociationsService;
import com.raffleease.raffleease.Domains.Auth.DTOs.Register.RegisterRequest;
import com.raffleease.raffleease.Domains.Auth.DTOs.Register.RegisterResponse;
import com.raffleease.raffleease.Domains.Auth.Model.VerificationToken;
import com.raffleease.raffleease.Domains.Auth.Repository.VerificationTokenRepository;
import com.raffleease.raffleease.Domains.Auth.Services.RegisterService;
import com.raffleease.raffleease.Domains.Notifications.Services.EmailsService;
import com.raffleease.raffleease.Domains.Users.Model.User;
import com.raffleease.raffleease.Domains.Users.Services.UsersService;
import com.raffleease.raffleease.Common.Exceptions.CustomExceptions.DatabaseException;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDateTime;
import java.util.UUID;

import static com.raffleease.raffleease.Common.Constants.Constants.EMAIL_VERIFICATION_EXPIRATION_MINUTES;
import static com.raffleease.raffleease.Domains.Associations.Model.AssociationRole.ADMIN;

@RequiredArgsConstructor
@Service
public class RegisterServiceImpl implements RegisterService {
    private final AssociationsService associationsService;
    private final UsersService usersService;
    private final PasswordEncoder passwordEncoder;
    private final VerificationTokenRepository verificationTokenRepository;
    private final EmailsService emailsService;
    private final CorsProperties corsProperties;

    @Value("${spring.application.security.jwt.refresh_token_expiration}")
    private Long refreshTokenExpiration;

    @Transactional
    @Override
    public RegisterResponse register(RegisterRequest request, HttpServletResponse response) {
        String encodedPassword = passwordEncoder.encode(request.userData().getPassword());
        User user = usersService.createUser(request.userData(), encodedPassword, false);
        Association association = associationsService.create(request.associationData());
        associationsService.createAssociationMembership(association, user, ADMIN);
        handleUserVerification(user);
        return toRegisterResponse(user);
    }

    private void handleUserVerification(User user) {
        VerificationToken verificationToken = createVerificationToken(user);
        String verificationLink = UriComponentsBuilder.fromHttpUrl(corsProperties.getClientAsList().get(0))
                .path("/auth/email-verification")
                .queryParam("token", verificationToken.getToken())
                .build()
                .toUriString();
        emailsService.sendEmailVerificationEmail(user, verificationLink);
    }

    private VerificationToken createVerificationToken(User user) {
        try {
            return verificationTokenRepository.save(VerificationToken.builder()
                    .token(UUID.randomUUID().toString())
                    .user(user)
                    .expiryDate(LocalDateTime.now().plusMinutes(EMAIL_VERIFICATION_EXPIRATION_MINUTES))
                    .build());
        } catch (DataAccessException ex) {
            throw new DatabaseException("Database error occurred while saving verification token");
        }
    }

    private RegisterResponse toRegisterResponse(User user) {
        return RegisterResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .build();
    }
}