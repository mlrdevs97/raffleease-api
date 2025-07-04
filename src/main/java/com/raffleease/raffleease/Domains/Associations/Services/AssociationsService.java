package com.raffleease.raffleease.Domains.Associations.Services;

import com.raffleease.raffleease.Domains.Associations.Model.Association;
import com.raffleease.raffleease.Domains.Associations.Model.AssociationRole;
import com.raffleease.raffleease.Domains.Auth.DTOs.Register.RegisterAssociationData;
import com.raffleease.raffleease.Domains.Users.Model.User;

public interface AssociationsService {
    /*
     * Creates a new association.
     * Used during the registration process.
     * 
     * @param associationData the association data
     * @return the created association
     */
    Association create(RegisterAssociationData associationData);

    /*
     * Finds an association by its ID.
     * 
     * @param id the ID of the association
     * @return the association
     * @throws NotFoundException if the association is not found
     */
    Association findById(Long id);

    /*
     * Creates a new membership for a user in an association.
     * Used during the registration process or when a new user account is created by an admin.
     * 
     * @param association the association
     * @param user the user
     * @param role the role
     */
    void createAssociationMembership(Association association, User user, AssociationRole role);
}
