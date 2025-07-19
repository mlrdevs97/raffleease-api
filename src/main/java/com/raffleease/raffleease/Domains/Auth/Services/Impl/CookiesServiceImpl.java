package com.raffleease.raffleease.Domains.Auth.Services.Impl;

import com.raffleease.raffleease.Domains.Auth.Services.CookiesService;
import com.raffleease.raffleease.Common.Exceptions.CustomExceptions.AuthorizationException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

import java.util.Objects;

import static org.springframework.http.HttpHeaders.SET_COOKIE;

@RequiredArgsConstructor
@Service
public class CookiesServiceImpl implements CookiesService {

    @Value("${spring.application.security.cookie_domain}")
    private String cookieDomain;

    public void addCookie(HttpServletResponse response, String name, String value, long maxAge) {
        ResponseCookie.ResponseCookieBuilder cookieBuilder = ResponseCookie.from(name, value)
                .domain(cookieDomain)
                .sameSite("None")
                .secure(true)
                .httpOnly(true)
                .path("/")
                .maxAge(maxAge);

        ResponseCookie cookie = cookieBuilder.build();
        response.addHeader(SET_COOKIE, cookie.toString());
    }

    public void deleteCookie(HttpServletResponse response, String name) {
        addCookie(response, name, "", 0);
    }

    @Override
    public String getCookieValue(HttpServletRequest request, String cookieName) {
        Cookie cookie = getCookie(request, cookieName);
        return cookie.getValue();
    }

    private Cookie getCookie(HttpServletRequest request, String cookieName) {
        Cookie[] cookies = request.getCookies();
        if (Objects.isNull(cookies) || cookies.length == 0) {
            throw new AuthorizationException("No cookies found in the request");
        }

        for (Cookie cookie : cookies) {
            if (Objects.nonNull(cookie) && cookie.getName().equals(cookieName)) {
                return cookie;
            }
        }
        throw new AuthorizationException("Cookie with name <" + cookieName + "> not found");
    }
}
