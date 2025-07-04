package com.raffleease.raffleease.Domains.Associations.Mappers.Impl;

import com.raffleease.raffleease.Domains.Associations.DTO.AddressDTO;
import com.raffleease.raffleease.Domains.Associations.Mappers.AddressMapper;
import com.raffleease.raffleease.Domains.Associations.Model.Address;
import com.raffleease.raffleease.Domains.Auth.DTOs.Register.RegisterAddressData;
import org.springframework.stereotype.Service;

@Service
public class AddressMapperImpl implements AddressMapper {
    public Address toAddress(RegisterAddressData addressData) {
        return Address.builder()
                .placeId(addressData.placeId())
                .city(addressData.city())
                .latitude(addressData.latitude())
                .longitude(addressData.longitude())
                .province(addressData.province())
                .zipCode(addressData.zipCode())
                .formattedAddress(addressData.formattedAddress())
                .build();
    }

    public AddressDTO fromAddress(Address address) {
        return AddressDTO.builder()
                .placeId(address.getPlaceId())
                .formattedAddress(address.getFormattedAddress())
                .latitude(address.getLatitude())
                .longitude(address.getLongitude())
                .city(address.getCity())
                .province(address.getProvince())
                .zipCode(address.getZipCode())
                .build();
    }
}