package com.raffleease.raffleease.Domains.Tokens.Services;

import java.security.Key;
import java.util.Date;
import java.util.function.Function;

import io.jsonwebtoken.Claims;

public interface TokensQueryService {

    /**
     * Extracts the subject (user ID) from a JWT token.
     * 
     * @param token the JWT token to parse
     * @return the subject claim value representing the user ID
     */
    String getSubject(String token);

    /**
     * Extracts the unique token identifier from a JWT token.
     * 
     * @param token the JWT token to parse
     * @return the jti (JWT ID) claim value used for token blacklisting
     */
    String getTokenId(String token);

    /**
     * Extracts a specific claim from a JWT token using a custom resolver function.
     * 
     * This generic method allows extraction of any claim from the JWT payload
     * by providing a function that specifies which claim to retrieve.
     * 
     * @param <T> the type of the claim value to extract
     * @param token the JWT token to parse
     * @param claimsResolver function that extracts the desired claim from JWT claims
     * @return the extracted claim value of type T
     */
    <T> T getClaim(String token, Function<Claims, T> claimsResolver);

    /**
     * Returns the cryptographic key used for JWT token signing and verification.
     * 
     * @return the HMAC signing key derived from the application's secret key
     */
    Key getSignInKey();

    /**
     * Returns the configured expiration time for access tokens.
     * 
     * @return access token expiration time in milliseconds
     */
    Long getAccessTokenExpirationValue();

    /**
     * Returns the configured expiration time for refresh tokens.
     * 
     * @return refresh token expiration time in milliseconds
     */
    Long getRefreshTokenExpirationValue();

    /**
     * Extracts the expiration date from a JWT token.
     * 
     * @param token the JWT token to parse
     * @return the expiration date of the token
     */
    Date getExpiration(String token);
}
