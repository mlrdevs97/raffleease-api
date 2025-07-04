package com.raffleease.raffleease.Domains.Tokens.Services;

public interface TokensCreateService {
    /**
     * Generates an access token for a user.
     * Used during the login or token refresh process.
     * 
     * @param userId the ID of the user to generate the token for
     * @return the generated access token
     */
    String generateAccessToken(Long userId);

    /**
     * Generates a refresh token for a user.
     * Used during the login or token refresh process.
     * The refresh token is used to refresh the access token.
     * 
     * @param userId the ID of the user to generate the token for
     * @return the generated refresh token
     */
    String generateRefreshToken(Long userId);
}
