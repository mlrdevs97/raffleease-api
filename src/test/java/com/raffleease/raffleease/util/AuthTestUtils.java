package com.raffleease.raffleease.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.raffleease.raffleease.Domains.Associations.Model.Association;
import com.raffleease.raffleease.Domains.Associations.Model.AssociationMembership;
import com.raffleease.raffleease.Domains.Associations.Model.AssociationRole;
import com.raffleease.raffleease.Domains.Associations.Repository.AssociationsMembershipsRepository;
import com.raffleease.raffleease.Domains.Associations.Repository.AssociationsRepository;
import com.raffleease.raffleease.Domains.Users.Model.User;
import com.raffleease.raffleease.Domains.Users.Repository.UsersRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.test.web.servlet.ResultActions;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Utility class for creating authenticated test data.
 * Provides common functionality for tests that require authentication.
 */
@Component
@RequiredArgsConstructor
public class AuthTestUtils {
    
    private final UsersRepository usersRepository;
    private final AssociationsRepository associationsRepository;
    private final AssociationsMembershipsRepository membershipsRepository;
    private final PasswordEncoder passwordEncoder;
    private final EntityManager entityManager;
    
    private static final String DEFAULT_TEST_PASSWORD = "mySecurePassword#123";

    /**
     * Creates a complete authenticated user setup with association and membership.
     * 
     * @return AuthTestData containing all related entities
     */
    public AuthTestData createAuthenticatedUser() {
        return createAuthenticatedUser(true, AssociationRole.ADMIN);
    }

    /**
     * Creates a complete authenticated user setup with specified parameters.
     * 
     * @param userEnabled whether the user account should be enabled
     * @param role the association role for the user
     * @return AuthTestData containing all related entities
     */
    public AuthTestData createAuthenticatedUser(boolean userEnabled, AssociationRole role) {
        // Generate unique identifier for this test instance
        String uniqueId = String.valueOf(System.currentTimeMillis());
        
        // Create and persist association with address
        Association association = TestDataBuilder.association()
                .name("Test Auth Association " + uniqueId)
                .email("auth-test-" + uniqueId + "@example.com")
                .phoneNumber("+1234" + String.format("%06d", Math.abs(uniqueId.hashCode() % 1000000)))
                .build();
        association = associationsRepository.save(association);

        // Create and persist user with encoded password
        String encodedPassword = passwordEncoder.encode(DEFAULT_TEST_PASSWORD);
        User user = TestDataBuilder.user()
                .userName("authtestuser" + uniqueId)
                .email("authtest" + uniqueId + "@example.com")
                .phoneNumber("+1", "987" + String.format("%06d", Math.abs((uniqueId + "user").hashCode() % 1000000)))
                .password(encodedPassword)
                .enabled(userEnabled)
                .build();
        user = usersRepository.save(user);

        // Create and persist membership linking user to association
        AssociationMembership membership = TestDataBuilder.membership()
                .user(user)
                .association(association)
                .role(role)
                .build();
        membership = membershipsRepository.save(membership);

        // Refresh entities to ensure they have all persisted data
        entityManager.flush();
        entityManager.refresh(user);
        entityManager.refresh(association);
        entityManager.refresh(membership);

        // Verify test data is properly set up
        assertThat(user.getId()).isNotNull();
        assertThat(association.getId()).isNotNull();
        assertThat(membership.getId()).isNotNull();
        assertThat(user.isEnabled()).isEqualTo(userEnabled);

        return new AuthTestData(user, association, membership, DEFAULT_TEST_PASSWORD);
    }

    /**
     * Creates an authenticated user with custom credentials.
     * 
     * @param username custom username
     * @param email custom email
     * @param password custom plain text password
     * @return AuthTestData containing all related entities
     */
    public AuthTestData createAuthenticatedUserWithCredentials(String username, String email, String password) {
        // Create and persist association with unique name based on username
        Association association = TestDataBuilder.association()
                .name("Custom Auth Association " + username)
                .email("custom-auth-" + username + "@example.com")
                .phoneNumber("+1555" + String.format("%06d", Math.abs(username.hashCode() % 1000000)))
                .build();
        association = associationsRepository.save(association);

        // Create and persist user with custom credentials and unique phone number
        String encodedPassword = passwordEncoder.encode(password);
        User user = TestDataBuilder.user()
                .userName(username)
                .email(email)
                .phoneNumber("+1", "987" + String.format("%06d", Math.abs((username + "phone").hashCode() % 1000000)))
                .password(encodedPassword)
                .enabled(true)
                .build();
        user = usersRepository.save(user);

        // Create membership
        AssociationMembership membership = TestDataBuilder.membership()
                .user(user)
                .association(association)
                .role(AssociationRole.ADMIN)
                .build();
        membership = membershipsRepository.save(membership);

        // Refresh entities
        entityManager.flush();
        entityManager.refresh(user);
        entityManager.refresh(association);
        entityManager.refresh(membership);

        return new AuthTestData(user, association, membership, password);
    }

    /**
     * Creates a disabled user account for testing authentication failures.
     * 
     * @return AuthTestData with disabled user
     */
    public AuthTestData createDisabledUser() {
        return createAuthenticatedUser(false, AssociationRole.ADMIN);
    }

    /**
     * Creates a user with MEMBER role instead of ADMIN.
     * 
     * @return AuthTestData with member role
     */
    public AuthTestData createMemberUser() {
        return createAuthenticatedUser(true, AssociationRole.MEMBER);
    }

    /**
     * Creates a second authenticated user in the same association as the provided association.
     * Useful for testing multi-user scenarios within the same association.
     * 
     * @param association the association to add the new user to
     * @return AuthTestData containing the new user and existing association
     */
    public AuthTestData createAuthenticatedUserInSameAssociation(Association association) {
        return createAuthenticatedUserInSameAssociation(association, AssociationRole.MEMBER);
    }

    /**
     * Creates a second authenticated user in the same association with the specified role.
     * Useful for testing multi-user scenarios within the same association.
     * 
     * @param association the association to add the new user to
     * @param role the association role for the new user
     * @return AuthTestData containing the new user and existing association
     */
    public AuthTestData createAuthenticatedUserInSameAssociation(Association association, AssociationRole role) {
        // Generate unique identifier for this test instance
        String uniqueId = String.valueOf(System.currentTimeMillis());
        
        // Create and persist user with encoded password
        String encodedPassword = passwordEncoder.encode(DEFAULT_TEST_PASSWORD);
        User user = TestDataBuilder.user()
                .userName("seconduser" + uniqueId)
                .email("seconduser" + uniqueId + "@example.com")
                .phoneNumber("+1", "888" + String.format("%06d", Math.abs((uniqueId + "second").hashCode() % 1000000)))
                .password(encodedPassword)
                .enabled(true)
                .build();
        user = usersRepository.save(user);

        // Create and persist membership linking user to existing association
        AssociationMembership membership = TestDataBuilder.membership()
                .user(user)
                .association(association)
                .role(role)
                .build();
        membership = membershipsRepository.save(membership);

        // Refresh entities to ensure they have all persisted data
        entityManager.flush();
        entityManager.refresh(user);
        entityManager.refresh(membership);

        // Verify test data is properly set up
        assertThat(user.getId()).isNotNull();
        assertThat(membership.getId()).isNotNull();
        assertThat(user.isEnabled()).isTrue();

        return new AuthTestData(user, association, membership, DEFAULT_TEST_PASSWORD);
    }

    /**
     * Extracts the user ID from a JSON response containing a user object.
     * 
     * @param result the ResultActions from a MockMvc call
     * @return the user ID from the response
     * @throws Exception if extraction fails
     */
    public Long extractUserIdFromResponse(ResultActions result) throws Exception {
        String responseJson = result.andReturn().getResponse().getContentAsString();
        ObjectMapper objectMapper = new ObjectMapper();
        
        try {
            // Parse the JSON response to extract the user ID from data.id
            var jsonNode = objectMapper.readTree(responseJson);
            var dataNode = jsonNode.get("data");
            if (dataNode != null && dataNode.has("id")) {
                return dataNode.get("id").asLong();
            }
            throw new RuntimeException("User ID not found in response: " + responseJson);
        } catch (Exception e) {
            throw new RuntimeException("Failed to extract user ID from response: " + responseJson, e);
        }
    }

    /**
     * Gets the association role for a user.
     * 
     * @param user the user to get the role for
     * @return the association role of the user
     */
    public AssociationRole getUserRoleInAssociation(User user) {
        AssociationMembership membership = membershipsRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("No membership found for user: " + user.getId()));
        return membership.getRole();
    }

    /**
     * Sets the association role for a user.
     * Useful for testing scenarios where you need to directly manipulate user roles.
     * 
     * @param user the user to set the role for
     * @param role the new role to assign
     */
    public void setUserRoleInAssociation(User user, AssociationRole role) {
        AssociationMembership membership = membershipsRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("No membership found for user: " + user.getId()));
        membership.setRole(role);
        membershipsRepository.save(membership);
        
        // Flush to ensure the change is persisted
        entityManager.flush();
        entityManager.refresh(membership);
    }

    /**
     * Record to hold test authentication data for clean organization.
     */
    public record AuthTestData(
            User user, 
            Association association, 
            AssociationMembership membership,
            String plainTextPassword
    ) {}
} 