package com.raffleease.raffleease.Domains.Associations.Services.Impl;

import com.raffleease.raffleease.Domains.Associations.Mappers.AssociationsMapper;
import com.raffleease.raffleease.Domains.Associations.Model.Association;
import com.raffleease.raffleease.Domains.Associations.Model.AssociationMembership;
import com.raffleease.raffleease.Domains.Associations.Model.AssociationRole;
import com.raffleease.raffleease.Domains.Associations.Repository.AssociationsMembershipsRepository;
import com.raffleease.raffleease.Domains.Associations.Repository.AssociationsRepository;
import com.raffleease.raffleease.Domains.Associations.Services.AssociationsMembershipService;
import com.raffleease.raffleease.Domains.Associations.Services.AssociationsService;
import com.raffleease.raffleease.Domains.Auth.DTOs.Register.RegisterAssociationData;
import com.raffleease.raffleease.Domains.Users.Model.User;
import com.raffleease.raffleease.Common.Exceptions.CustomExceptions.DatabaseException;
import com.raffleease.raffleease.Common.Exceptions.CustomExceptions.NotFoundException;
import com.raffleease.raffleease.Common.Exceptions.CustomExceptions.UniqueConstraintViolationException;
import com.raffleease.raffleease.Common.Utils.ConstraintViolationParser;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.Optional;

@RequiredArgsConstructor
@Service
public class AssociationsServiceImpl implements AssociationsService {
    private final AssociationsRepository associationsRepository;
    private final AssociationsMembershipService membershipService;
    private final AssociationsMapper mapper;

    @Transactional
    @Override
    public Association create(RegisterAssociationData associationData) {
        return save(mapper.toAssociation(associationData));
    }

    @Override
    public Association findById(Long id) {
        return associationsRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Association with id <" + id + "> not found"));
    }

    @Transactional
    @Override
    public void createAssociationMembership(Association association, User user, AssociationRole role) {
        AssociationMembership membership = membershipService.createMembership(association, user, role);
        association.getMemberships().add(membership);
        save(association);
    }

    private Association save(Association entity) {
        try {
            return associationsRepository.save(entity);
        } catch (DataIntegrityViolationException ex) {
            Optional<String> constraintName = ConstraintViolationParser.extractConstraintName(ex);
            if (constraintName.isPresent()) {
                throw new UniqueConstraintViolationException(constraintName.get(), "Unique constraint violated: " + constraintName.get());
            } else {
                throw ex;
            }
        } catch (DataAccessException ex) {
            throw new DatabaseException("Database error occurred while saving association: " + ex.getMessage());
        }
    }
}
