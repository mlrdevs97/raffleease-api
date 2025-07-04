package com.raffleease.raffleease.Domains.Auth.Validations.Interceptors;

import com.raffleease.raffleease.Common.Exceptions.CustomExceptions.AuthenticationException;
import com.raffleease.raffleease.Common.Exceptions.CustomExceptions.AuthorizationException;
import com.raffleease.raffleease.Domains.Associations.Model.Association;
import com.raffleease.raffleease.Domains.Associations.Services.AssociationsMembershipService;
import com.raffleease.raffleease.Domains.Associations.Services.AssociationsService;
import com.raffleease.raffleease.Domains.Auth.Validations.ValidateAssociationAccess;
import com.raffleease.raffleease.Domains.Users.Model.User;
import com.raffleease.raffleease.Domains.Users.Services.UsersService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Map;

import static org.springframework.web.servlet.HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE;

@RequiredArgsConstructor
@Component
public class AssociationAccessInterceptor implements HandlerInterceptor {
    private final AssociationsService associationsService;
    private final UsersService usersService;
    private final AssociationsMembershipService membershipsService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!(handler instanceof HandlerMethod method)) return true;
        if (!method.getBeanType().isAnnotationPresent(ValidateAssociationAccess.class)) return true;

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new AuthenticationException("Authentication required");
        }

        Map<String, String> pathVariables = (Map<String, String>) request.getAttribute(URI_TEMPLATE_VARIABLES_ATTRIBUTE);
        String associationIdStr = pathVariables.get("associationId");
        if (associationIdStr == null) {
            throw new AuthorizationException("Association ID is required");
        }

        Long associationId;
        try {
            associationId = Long.parseLong(associationIdStr);
        } catch (NumberFormatException ex) {
            throw new AuthorizationException("Invalid association ID format");
        }

        String identifier = auth.getName();
        User user = usersService.findByIdentifier(identifier);
        
        try {
            Association association = associationsService.findById(associationId);
            membershipsService.validateIsMember(association, user);
            return true;
        } catch (Exception ex) {
            throw new AuthorizationException("You are not allowed to access this association");
        }
    }
}