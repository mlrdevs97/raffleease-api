package com.raffleease.raffleease.Common.Security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.raffleease.raffleease.Common.Responses.ErrorResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

import static com.raffleease.raffleease.Common.Exceptions.ErrorCodes.ACCESS_DENIED;
import static org.springframework.http.HttpStatus.FORBIDDEN;

@RequiredArgsConstructor
@Component
public class CustomAccessDeniedHandler implements AccessDeniedHandler {
    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException) throws IOException {
        ErrorResponse errorResponse = new ErrorResponse(
                "Access denied",
                FORBIDDEN.value(),
                FORBIDDEN.getReasonPhrase(),
                ACCESS_DENIED
        );

        response.setStatus(FORBIDDEN.value());
        response.setContentType("application/json");
        new ObjectMapper().writeValue(response.getOutputStream(), errorResponse);
    }
}