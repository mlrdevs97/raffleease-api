package com.raffleease.raffleease.Domains.Auth.Services;

import com.raffleease.raffleease.Domains.Auth.DTOs.Register.RegisterRequest;
import com.raffleease.raffleease.Domains.Auth.DTOs.Register.RegisterResponse;
import jakarta.servlet.http.HttpServletResponse;

public interface RegisterService {
    /*
     * Registers a new user and returns a registration response. 
     * After the user is registered, user should verify their email address before they can login.
     * 
     * @param request the registration request
     * @param response the response
     * @return the registration response
     */
    RegisterResponse register(RegisterRequest request, HttpServletResponse response);
}
