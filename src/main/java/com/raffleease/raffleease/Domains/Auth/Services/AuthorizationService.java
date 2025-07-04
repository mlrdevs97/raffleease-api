package com.raffleease.raffleease.Domains.Auth.Services;

import com.raffleease.raffleease.Domains.Associations.Model.AssociationRole;

public interface AuthorizationService {
    /**
     * Check if the current authenticated user has the required role for the given association
     * 
     * @param associationId The ID of the association to check the role for
     * @param requiredRole The role to check for
     * @return True if the user has the required role, false otherwise
     */
    boolean hasRole(Long associationId, AssociationRole requiredRole);
    
    /**
     * Check if the current authenticated user has access to modify the target user
     * 
     * @param associationId The ID of the association to check the role for
     * @param targetUserId The ID of the user to check access for
     * @param allowSelfAccess True if the user can modify their own account, false otherwise
     * @return True if the user has access to modify the target user, false otherwise
     */
    boolean canModifyUser(Long associationId, Long targetUserId, boolean allowSelfAccess);
    
    /**
     * Check if the current authenticated user is the same as the target user
     * 
     * @param targetUserId The ID of the user to check if they are the same as the current user
     * @return True if the current user is the same as the target user, false otherwise
     */
    boolean isSameUser(Long targetUserId);
    
    /**
     * Verify that the current user has the required role, throw exception if not
     * 
     * @param associationId The ID of the association to check the role for
     * @param requiredRole The role to check for
     * @param message The message to throw if the user does not have the required role
     */
    void requireRole(Long associationId, AssociationRole requiredRole, String message);
}