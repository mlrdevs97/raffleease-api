package com.raffleease.raffleease.Domains.Tokens.Services;

public interface BlackListService {

    /**
     * Adds a JWT token to the blacklist with a specified expiration time.
     * 
     * This method blacklists a token by storing its ID in Redis with a TTL that matches the token's remaining lifetime.
     * Once blacklisted, the token cannot be used for authentication until it naturally expires.
     * 
     * @param tokenId the unique identifier of the token (jti claim from JWT)
     * @param expiration the expiration time in milliseconds from current time
     * @throws IllegalArgumentException if tokenId is null, empty, or expiration is invalid
     * @throws RuntimeException if Redis operations fail or connection issues occur
     */
    void addTokenToBlackList(String tokenId, Long expiration);

    /**
     * Checks whether a JWT token is currently blacklisted.
     * 
     * This method performs a fast lookup in Redis to determine if a token ID exists in the blacklist.
     * It's used during token validation to ensure that revoked tokens are rejected before processing authentication.
     * 
     * @param tokenId the unique identifier of the token to check (jti claim from JWT)
     * @return true if the token is blacklisted, false if it's valid or not found
     * @throws IllegalArgumentException if tokenId is null or empty
     */
    boolean isTokenBlackListed(String tokenId);
}
