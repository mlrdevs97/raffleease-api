package com.raffleease.raffleease.Domains.Associations.Mappers.Impl;

import com.raffleease.raffleease.Domains.Associations.DTO.AssociationDTO;
import com.raffleease.raffleease.Domains.Associations.Mappers.AddressMapper;
import com.raffleease.raffleease.Domains.Associations.Mappers.AssociationsMapper;
import com.raffleease.raffleease.Domains.Associations.Model.Association;
import com.raffleease.raffleease.Domains.Auth.DTOs.Register.RegisterAssociationData;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class AssociationsMapperImpl implements AssociationsMapper {
    private final AddressMapper addressMapper;

    public Association toAssociation(RegisterAssociationData data) {
        String phoneNumber = Objects.nonNull(data.phoneNumber())
                ? data.phoneNumber().prefix() + data.phoneNumber().nationalNumber()
                : null;

        return Association.builder()
                .name(data.associationName())
                .email(data.email())
                .phoneNumber(phoneNumber)
                .address(addressMapper.toAddress(data.addressData()))
                .memberships(new ArrayList<>())
                .description(data.description())
                .build();
    }

    public AssociationDTO fromAssociation(Association association) {
        return AssociationDTO.builder()
                .id(association.getId())
                .associationName(association.getName())
                .description(association.getDescription())
                .email(association.getEmail())
                .phoneNumber(association.getPhoneNumber())
                .addressData(addressMapper.fromAddress(association.getAddress()))
                .build();
    }
}