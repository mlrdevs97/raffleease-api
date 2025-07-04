package com.raffleease.raffleease.Domains.Associations.Mappers;

import com.raffleease.raffleease.Domains.Associations.DTO.AddressDTO;
import com.raffleease.raffleease.Domains.Associations.Model.Address;
import com.raffleease.raffleease.Domains.Auth.DTOs.Register.RegisterAddressData;

public interface AddressMapper {
    Address toAddress(RegisterAddressData addressData);
    AddressDTO fromAddress(Address address);
}
