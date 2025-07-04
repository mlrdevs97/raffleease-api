package com.raffleease.raffleease.Domains.Auth.Services;

public interface AuthValidationService {
    /*
     * Validates if the user is authenticated and throws an exception if not.
     * The client can use this method to ensure the user is authenticated.
     * 
     * @throws AuthorizationException if the user is not authenticated
     */
    void isUserAuthenticated();
}
