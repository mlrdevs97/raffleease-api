package com.raffleease.raffleease.Domains.Auth.Services;

import com.raffleease.raffleease.Domains.Auth.DTOs.LoginRequest;
import com.raffleease.raffleease.Domains.Auth.DTOs.AuthResponse;
import jakarta.servlet.http.HttpServletResponse;

public interface LoginService {
    /*
     * Logs in a user and returns an authentication response.
     * 
     * @param request the login request
     * @param response the response
     * @return the authentication response
     */
    AuthResponse login(LoginRequest request, HttpServletResponse response);
}
