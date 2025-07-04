package com.raffleease.raffleease.Domains.Associations.Mappers;

import com.raffleease.raffleease.Domains.Associations.DTO.AssociationDTO;
import com.raffleease.raffleease.Domains.Associations.Model.Association;
import com.raffleease.raffleease.Domains.Auth.DTOs.Register.RegisterAssociationData;

public interface AssociationsMapper {
    Association toAssociation(RegisterAssociationData associationData);
    AssociationDTO fromAssociation(Association association);
}
