package com.raffleease.raffleease.Domains.Associations.Model;

import jakarta.persistence.*;
import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
@Entity
@Table(name = "Addresses")
public class Address {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String placeId;

    @Column(nullable = false)
    private String formattedAddress;

    @Column(nullable = false, precision = 9)
    private Double latitude;

    @Column(nullable = false, precision = 9)
    private Double longitude;

    @Column(length = 100)
    private String city;

    @Column(length = 100)
    private String province;

    @Column(length = 10)
    private String zipCode;
}
