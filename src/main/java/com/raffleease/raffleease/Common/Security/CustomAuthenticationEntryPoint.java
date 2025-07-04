package com.raffleease.raffleease.Common.Security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.raffleease.raffleease.Common.Exceptions.ErrorCodes;
import com.raffleease.raffleease.Common.Responses.ErrorResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@RequiredArgsConstructor
@Component
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {
    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException {
        ErrorResponse errorResponse = new ErrorResponse(
                "Authentication required",
                UNAUTHORIZED.value(),
                UNAUTHORIZED.getReasonPhrase(),
                ErrorCodes.UNAUTHORIZED
        );

        response.setStatus(UNAUTHORIZED.value());
        response.setContentType("application/json");
        objectMapper.writeValue(response.getOutputStream(), errorResponse);
    }
}