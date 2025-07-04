package com.raffleease.raffleease.Domains.Tokens.Services;

import com.raffleease.raffleease.Domains.Auth.DTOs.AuthResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public interface TokensManagementService {

    /**
     * Revokes a JWT token by adding it to the blacklist.
     * 
     * This method invalidates a JWT token by extracting its ID and adding it to the blacklist with the remaining expiration time.
     * Once revoked, the token cannot be used for authentication until it naturally expires.
     * 
     * @param token the JWT token to be revoked (must be valid and non-expired)
     * @throws com.raffleease.raffleease.Common.Exceptions.CustomExceptions.AuthorizationException
     *         if the token is invalid, expired, or malformed
     * @throws RuntimeException if blacklisting operation fails
     */
    void revoke(String token);

    /**
     * Refreshes both access and refresh tokens for a user session.
     * 
     * @param request the HTTP request containing Authorization header and refresh token cookie
     * @param response the HTTP response where the new refresh token cookie will be set
     * @return AuthResponse containing the new access token and user's association ID
     * @throws com.raffleease.raffleease.Common.Exceptions.CustomExceptions.AuthorizationException
     *         if tokens are invalid, user doesn't exist, or association membership is not found
     * @throws IllegalArgumentException if required tokens are missing from the request
     */
    AuthResponse refresh(HttpServletRequest request, HttpServletResponse response);

    /**
     * Extracts the JWT access token from the HTTP request Authorization header.
     * 
     * This method parses the Authorization header to extract the Bearer token, removing the "Bearer " prefix to return the raw JWT token string.
     * It expects the standard HTTP Authorization header format: "Bearer <token>".
     * 
     * @param request the HTTP request containing the Authorization header
     * @return the extracted JWT token string without the "Bearer " prefix
     * @throws StringIndexOutOfBoundsException if the Authorization header is shorter than expected
     * @throws NullPointerException if the Authorization header is missing
     * @throws IllegalArgumentException if the Authorization header is missing
     */
    String extractTokenFromRequest(HttpServletRequest request);
}
