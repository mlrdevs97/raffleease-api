package com.raffleease.raffleease.Domains.Associations.Repository;

import com.raffleease.raffleease.Domains.Associations.Model.Association;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AssociationsRepository extends JpaRepository<Association, Long> {
}
