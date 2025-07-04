package com.raffleease.raffleease.Domains.Customers.Model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import lombok.*;

import static jakarta.persistence.GenerationType.IDENTITY;

@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
@Builder
@Entity
public class CustomersPhoneNumber {
    @Id
    @GeneratedValue(strategy = IDENTITY)
    private Long id;
    private String prefix;
    private String nationalNumber;
}