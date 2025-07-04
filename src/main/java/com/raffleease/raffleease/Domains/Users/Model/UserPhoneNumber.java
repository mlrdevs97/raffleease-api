package com.raffleease.raffleease.Domains.Users.Model;

import jakarta.persistence.*;
import lombok.*;

import static jakarta.persistence.GenerationType.IDENTITY;

@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
@Builder
@Entity
@Table(name = "user_phone_numbers", uniqueConstraints = {
        @UniqueConstraint(name = "uk_user_phone", columnNames = {"prefix", "national_number"})
})
public class UserPhoneNumber {
    @Id
    @GeneratedValue(strategy = IDENTITY)
    private Long id;
    
    @Column(name = "prefix", nullable = false)
    private String prefix;
    
    @Column(name = "national_number", nullable = false)
    private String nationalNumber;
}
