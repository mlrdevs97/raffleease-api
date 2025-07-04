package com.raffleease.raffleease.Domains.Auth.Services;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public interface CookiesService {  
    /*
     * Adds a secure, httpOnly, sameSite cookie to the response.
     * 
     * @param response the response
     * @param name the name of the cookie
     * @param value the value of the cookie
     * @param maxAge the max age of the cookie
     */
    void addCookie(HttpServletResponse response, String name, String value, long maxAge);

    /*
     * Deletes a cookie from the response.
     * 
     * @param response the response
     * @param name the name of the cookie
     */
    void deleteCookie(HttpServletResponse response, String name);

    /*
     * Gets the value of a cookie from the request.
     * 
     * @param request the request
     * @param cookieName the name of the cookie
     * @return the value of the cookie
     */
    String getCookieValue(HttpServletRequest request, String cookieName);
}
