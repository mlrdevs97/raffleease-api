package com.raffleease.raffleease.Domains.Users.Services.Impls;

import com.raffleease.raffleease.Common.Models.UserBaseDTO;
import com.raffleease.raffleease.Common.Models.UserRegisterDTO;
import com.raffleease.raffleease.Domains.Associations.Model.AssociationRole;
import com.raffleease.raffleease.Domains.Associations.Services.AssociationsMembershipService;
import com.raffleease.raffleease.Domains.Users.DTOs.UserResponse;
import com.raffleease.raffleease.Domains.Users.DTOs.UserSearchFilters;
import com.raffleease.raffleease.Domains.Users.Mappers.UsersMapper;
import com.raffleease.raffleease.Domains.Users.Model.User;
import com.raffleease.raffleease.Domains.Users.Repository.UsersRepository;
import com.raffleease.raffleease.Domains.Users.Repository.UsersSearchRepository;
import com.raffleease.raffleease.Domains.Users.Services.UsersService;
import com.raffleease.raffleease.Common.Exceptions.CustomExceptions.DatabaseException;
import com.raffleease.raffleease.Common.Exceptions.CustomExceptions.NotFoundException;
import com.raffleease.raffleease.Common.Exceptions.CustomExceptions.UniqueConstraintViolationException;
import com.raffleease.raffleease.Common.Utils.ConstraintViolationParser;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.Optional;

@RequiredArgsConstructor
@Service
public class UsersServiceImpl implements UsersService {
    private final UsersRepository repository;
    private final UsersSearchRepository searchRepository;
    private final AssociationsMembershipService membershipService;
    private final UsersMapper mapper;

    @Transactional
    @Override
    public User createUser(UserRegisterDTO userData, String encodedPassword, boolean isEnabled) {
        return save(mapper.buildUser(userData, encodedPassword, isEnabled));
    }

    @Override
    public User updateUser(User user, UserBaseDTO userData) {
        if (Objects.nonNull(userData.getFirstName())) {
        user.setFirstName(userData.getFirstName());
        }
        if (Objects.nonNull(userData.getLastName())) {
            user.setLastName(userData.getLastName());
        }
        if (Objects.nonNull(userData.getUserName())) {
            user.setUserName(userData.getUserName());
        }
        return save(user);
    }

    @Override
    public User setUserEnabled(User user, boolean enabled) {
        user.setEnabled(enabled);
        return save(user);
    }

    @Override
    public User updatePassword(User user, String encodedPassword) {
        user.setPassword(encodedPassword);
        return save(user);
    }

    @Override
    public UserResponse getUserResponseById(Long userId) {
        User user = findById(userId);
        AssociationRole role = membershipService.getUserRoleInAssociation(user);
        return mapper.toUserResponse(user, role);
    }

    @Override
    public Page<UserResponse> search(Long associationId, UserSearchFilters searchFilters, Pageable pageable) {
        Page<User> usersPage = searchRepository.search(searchFilters, associationId, pageable);
        return usersPage.map(user -> {
            AssociationRole role = membershipService.getUserRoleInAssociation(user);
            return mapper.toUserResponse(user, role);
        });
    }

    @Override
    public User findByIdentifier(String identifier) {
        return repository.findByIdentifier(identifier).orElseThrow(
                () -> new NotFoundException("User not found with identifier: " + identifier)
        );
    }

    @Override
    public User getUserByEmail(String email) {
        return repository.findByEmail(email).orElseThrow(
                () -> new NotFoundException("User not found with email: " + email)
        );
    }

    @Override
    public User findById(Long id) {
        return repository.findById(id).orElseThrow(
                () -> new NotFoundException("User not found with id: " + id)
        );
    }

    @Override
    public boolean existsById(Long id) {
        try {
            findById(id);
            return true;
        } catch (NotFoundException ex) {
            return false;
        }
    }

     @Override
     public User getAuthenticatedUser() {
         Authentication auth = SecurityContextHolder.getContext().getAuthentication();
         String identifier = auth.getName();
         return findByIdentifier(identifier);
     }

    @Override
    public boolean existsByEmail(String email) {
        return repository.findByEmail(email).isPresent();
    }

    @Override
    public User save(User user) {
        try {
            return repository.saveAndFlush(user);
        } catch (DataIntegrityViolationException ex) {
            Optional<String> constraintName = ConstraintViolationParser.extractConstraintName(ex);
            if (constraintName.isPresent()) {
                throw new UniqueConstraintViolationException(constraintName.get(), "Unique constraint violated: " + constraintName.get());
            } else {
                throw ex;
            }
        } catch (DataAccessException ex) {
            throw new DatabaseException("Database error occurred while saving user: " + ex.getMessage());
        }
    }
}
