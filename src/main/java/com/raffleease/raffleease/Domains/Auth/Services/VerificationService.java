package com.raffleease.raffleease.Domains.Auth.Services;

public interface VerificationService {  
    /*
     * Verifies the email address of a user using a token sent to their email.
     * 
     * @param token the token sent to the user's email
     */
    public void verifyEmail(String token);
}
