package com.raffleease.raffleease.Domains.Customers.Mappers;

import com.raffleease.raffleease.Common.Models.PhoneNumberDTO;
import com.raffleease.raffleease.Domains.Customers.DTO.CustomerCreate;
import com.raffleease.raffleease.Domains.Customers.DTO.CustomerDTO;
import com.raffleease.raffleease.Domains.Customers.Model.Customer;
import com.raffleease.raffleease.Domains.Customers.Model.CustomersPhoneNumber;
import org.springframework.stereotype.Service;

@Service
public class CustomersMapper {
    public CustomerDTO fromCustomer(Customer customer) {
        PhoneNumberDTO phoneNumber = null;
        if (customer.getPhoneNumber() != null) {
            phoneNumber = PhoneNumberDTO.builder()
                    .prefix(customer.getPhoneNumber().getPrefix())
                    .nationalNumber(customer.getPhoneNumber().getNationalNumber())
                    .build();
        }

        return CustomerDTO.builder()
                .id(customer.getId())
                .fullName(customer.getFullName())
                .email(customer.getEmail())
                .phoneNumber(phoneNumber)
                .createdAt(customer.getCreatedAt())
                .updatedAt(customer.getUpdatedAt())
                .build();
    }

    public Customer toCustomer(CustomerCreate data) {
        CustomersPhoneNumber phoneNumber = null;
        if (data.phoneNumber() != null) {
            phoneNumber = CustomersPhoneNumber.builder()
                    .prefix(data.phoneNumber().prefix())
                    .nationalNumber(data.phoneNumber().nationalNumber())
                    .build();
        }

        return Customer.builder()
                .fullName(data.fullName())
                .email(data.email())
                .phoneNumber(phoneNumber)
                .build();
    }
}
