package com.raffleease.raffleease.Domains.Associations.Services.Impl;

import com.raffleease.raffleease.Common.Exceptions.CustomExceptions.BusinessException;
import com.raffleease.raffleease.Common.Exceptions.CustomExceptions.DatabaseException;
import com.raffleease.raffleease.Domains.Associations.Model.Association;
import com.raffleease.raffleease.Domains.Associations.Model.AssociationMembership;
import com.raffleease.raffleease.Domains.Associations.Model.AssociationRole;
import com.raffleease.raffleease.Domains.Associations.Repository.AssociationsMembershipsRepository;
import com.raffleease.raffleease.Domains.Associations.Services.AssociationsMembershipService;
import com.raffleease.raffleease.Domains.Users.Model.User;
import com.raffleease.raffleease.Common.Exceptions.CustomExceptions.AuthorizationException;
import com.raffleease.raffleease.Common.Exceptions.CustomExceptions.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class AssociationsMembershipServiceImpl implements AssociationsMembershipService {
    private final AssociationsMembershipsRepository repository;

    @Override
    public AssociationMembership createMembership(Association association, User user, AssociationRole role) {
        validateMembershipExists(association, user);
        return save(AssociationMembership.builder()
                .association(association)
                .user(user)
                .role(role)
                .build());
    }

    @Override
    public void validateIsMember(Association association, User user) {
        boolean isMember = repository.existsByAssociationAndUser(association, user);
        if (!isMember) {
            throw new AuthorizationException("User is not a member of the association");
        }
    }

    @Override
    public AssociationMembership findByUser(User user) {
        return repository.findByUser(user).orElseThrow(
                () -> new NotFoundException("No association membership was found for user <" + user.getId() + ">")
        );
    }

    @Override
    public AssociationRole getUserRoleInAssociation(User user) {
        AssociationMembership membership = findByUser(user);
        return membership.getRole();
    }

    @Override
    public AssociationMembership updateUserRole(User user, AssociationRole newRole) {
        AssociationMembership membership = findByUser(user);
        membership.setRole(newRole);
        return repository.save(membership);
    }

    private AssociationMembership save(AssociationMembership membership) {
        try {
            return repository.save(membership);
        } catch (DataAccessException ex) {
            throw new DatabaseException("Database error occurred while saving new membership for association: " + ex.getMessage());
        }
    }

    private void validateMembershipExists(Association association, User user) {
        if (repository.existsByAssociationAndUser(association, user)) {
            throw new BusinessException("User is already a member of the association");
        }
    }
}
