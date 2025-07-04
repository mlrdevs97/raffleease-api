package com.raffleease.raffleease.Common.Security;

import com.raffleease.raffleease.Domains.Tokens.Services.TokensQueryService;
import com.raffleease.raffleease.Domains.Tokens.Services.TokensValidateService;
import com.raffleease.raffleease.Domains.Users.Model.CustomUserDetails;
import com.raffleease.raffleease.Domains.Users.Model.User;
import com.raffleease.raffleease.Domains.Users.Services.UsersService;
import com.raffleease.raffleease.Common.Exceptions.CustomExceptions.AuthorizationException;
import com.raffleease.raffleease.Common.Exceptions.CustomExceptions.NotFoundException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
 import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Objects;

@RequiredArgsConstructor
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final TokensQueryService tokenQueryService;
    private final TokensValidateService tokenValidationService;
    private final UsersService usersService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        if (request.getServletPath().contains("/api/v1/auth/login") || request.getServletPath().contains("/api/v1/auth/register")) {
            filterChain.doFilter(request, response);
            return;
        }

        String authHeader = request.getHeader("Authorization");
        if (Objects.isNull(authHeader) || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String jwt = authHeader.substring(7);
        boolean isTokenValid;
        try {
            isTokenValid = tokenValidationService.isTokenValid(jwt);
        } catch (AuthorizationException ex) {
            isTokenValid = false;
        }

        if (!isTokenValid) {
            filterChain.doFilter(request, response);
            return;
        }

        if (Objects.nonNull(SecurityContextHolder.getContext().getAuthentication())) {
            filterChain.doFilter(request, response);
            return;
        }

        String subject = tokenQueryService.getSubject(jwt);
        Long userId = Long.parseLong(subject);
        User user;
        try {
            user = usersService.findById(userId);
        } catch (NotFoundException ex) {
            filterChain.doFilter(request, response);
            return;
        }

        UserDetails userDetails = CustomUserDetails.builder()
                .user(user)
                .identifier(user.getEmail())
                .build();

        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                userDetails,
                null,
                userDetails.getAuthorities()
        );

        authToken.setDetails(
                new WebAuthenticationDetailsSource().buildDetails(request)
        );

        SecurityContextHolder.getContext().setAuthentication(authToken);

        filterChain.doFilter(request, response);
    }
}