package com.raffleease.raffleease.Domains.Auth.Services.Impl;

import com.raffleease.raffleease.Domains.Associations.Model.AssociationRole;
import com.raffleease.raffleease.Domains.Associations.Services.AssociationsMembershipService;
import com.raffleease.raffleease.Domains.Auth.Services.AuthorizationService;
import com.raffleease.raffleease.Domains.Users.Model.User;
import com.raffleease.raffleease.Domains.Users.Services.UsersService;
import com.raffleease.raffleease.Common.Exceptions.CustomExceptions.AuthorizationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

import static com.raffleease.raffleease.Domains.Associations.Model.AssociationRole.*;

@Slf4j
@RequiredArgsConstructor
@Service
public class AuthorizationServiceImpl implements AuthorizationService {
    private final UsersService usersService;
    private final AssociationsMembershipService membershipService;
    
    // Role hierarchy: ADMIN > MEMBER > COLLABORATOR
    private static final Map<AssociationRole, Integer> ROLE_HIERARCHY = Map.of(
        COLLABORATOR, 1,
        MEMBER, 2,
        ADMIN, 3
    );

    @Override
    public boolean hasRole(Long associationId, AssociationRole requiredRole) {
        try {
            User user = usersService.getAuthenticatedUser();
            AssociationRole userRole = membershipService.getUserRoleInAssociation(user);
            return hasRoleHierarchy(userRole, requiredRole);
        } catch (Exception e) {
            log.debug("Failed to check role for association {}: {}", associationId, e.getMessage());
            return false;
        }
    }

    @Override
    public boolean canModifyUser(Long associationId, Long targetUserId, boolean allowSelfAccess) {
        User currentUser = usersService.getAuthenticatedUser();
        if (allowSelfAccess && currentUser.getId().equals(targetUserId)) {
            return true;
        }
        return hasRole(associationId, ADMIN);
    }

    @Override
    public boolean isSameUser(Long targetUserId) {
        User currentUser = usersService.getAuthenticatedUser();
        return currentUser.getId().equals(targetUserId);
    }

    @Override
    public void requireRole(Long associationId, AssociationRole requiredRole, String message) {
        if (!hasRole(associationId, requiredRole)) {
            User user = usersService.getAuthenticatedUser();
            AssociationRole userRole = membershipService.getUserRoleInAssociation(user);
            
            log.warn("Access denied for user {} with role {} trying to access resource requiring {}", 
                    user.getUserName(), userRole, requiredRole);
            
            throw new AuthorizationException(message);
        }
    }

    /**
     * Check if userRole has sufficient privileges compared to requiredRole
     * Based on hierarchy: ADMIN > MEMBER > COLLABORATOR
     */
    private boolean hasRoleHierarchy(AssociationRole userRole, AssociationRole requiredRole) {
        if (userRole == null || requiredRole == null) {
            return false;
        }
        
        Integer userLevel = ROLE_HIERARCHY.get(userRole);
        Integer requiredLevel = ROLE_HIERARCHY.get(requiredRole);
        
        return userLevel != null && requiredLevel != null && userLevel >= requiredLevel;
    }
} 