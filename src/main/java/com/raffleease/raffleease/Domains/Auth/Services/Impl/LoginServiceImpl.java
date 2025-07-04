package com.raffleease.raffleease.Domains.Auth.Services.Impl;

import com.raffleease.raffleease.Domains.Associations.Model.AssociationMembership;
import com.raffleease.raffleease.Domains.Associations.Services.AssociationsMembershipService;
import com.raffleease.raffleease.Domains.Auth.DTOs.LoginRequest;
import com.raffleease.raffleease.Domains.Auth.DTOs.AuthResponse;
import com.raffleease.raffleease.Domains.Auth.Services.CookiesService;
import com.raffleease.raffleease.Domains.Auth.Services.LoginService;
import com.raffleease.raffleease.Domains.Tokens.Services.TokensCreateService;
import com.raffleease.raffleease.Domains.Users.Model.User;
import com.raffleease.raffleease.Domains.Users.Services.UsersService;
import com.raffleease.raffleease.Common.Exceptions.CustomExceptions.AuthenticationException;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;

@Slf4j
@RequiredArgsConstructor
@Service
public class LoginServiceImpl implements LoginService {
    private final AuthenticationManager authenticationManager;
    private final TokensCreateService tokensCreateService;
    private final CookiesService cookiesService;
    private final UsersService usersService;
    private final AssociationsMembershipService membershipService;

    @Value("${spring.application.security.jwt.refresh_token_expiration}")
    private Long refreshTokenExpiration;

    public AuthResponse login(LoginRequest request, HttpServletResponse response) {
        authenticateCredentials(request.identifier(), request.password());
        User user = usersService.findByIdentifier(request.identifier());
        AssociationMembership membership = membershipService.findByUser(user);
        String accessToken = tokensCreateService.generateAccessToken(user.getId());
        String refreshToken = tokensCreateService.generateRefreshToken(user.getId());
        cookiesService.addCookie(response, "refresh_token", refreshToken, refreshTokenExpiration);
        return AuthResponse.builder()
                .accessToken(accessToken)
                .associationId(membership.getAssociation().getId())
                .userId(user.getId())
                .build();
    }

    private void authenticateCredentials(String identifier, String password) {
        try {
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(identifier, password));
        } catch (org.springframework.security.core.AuthenticationException ex) {
            throw new AuthenticationException("Authentication failed for provided credentials: " + ex.getMessage());
        }
    }
}
