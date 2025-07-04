package com.raffleease.raffleease.Domains.Tokens.Services;

public interface TokensValidateService {

    /**
     * Validates a JWT token and throws an exception if invalid.
     * 
     * Performs comprehensive token validation including expiration check, blacklist verification, and subject format validation.
     * This method is used when validation failures should halt processing.
     * 
     * @param token the JWT token to validate
     * @throws com.raffleease.raffleease.Common.Exceptions.CustomExceptions.AuthorizationException
     *         if the token is expired, blacklisted, malformed, or has invalid subject
     */
    void validateToken(String token);

    /**
     * Checks if a JWT token is valid without throwing exceptions.
     * 
     * Performs the same validation checks as validateToken but returns a boolean result instead of throwing exceptions.
     * Useful for conditional logic where you need to handle invalid tokens gracefully.
     * 
     * @param token the JWT token to check
     * @return true if the token is valid, false otherwise
     */
    boolean isTokenValid(String token);
}
