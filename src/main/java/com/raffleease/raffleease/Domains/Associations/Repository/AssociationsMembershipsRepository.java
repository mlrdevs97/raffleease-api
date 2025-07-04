package com.raffleease.raffleease.Domains.Associations.Repository;

import com.raffleease.raffleease.Domains.Associations.Model.Association;
import com.raffleease.raffleease.Domains.Associations.Model.AssociationMembership;
import com.raffleease.raffleease.Domains.Users.Model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AssociationsMembershipsRepository extends JpaRepository<AssociationMembership, Long> {
    boolean existsByAssociationAndUser(Association association, User user);
    Optional<AssociationMembership> findByUser(User user);
}
