package com.raffleease.raffleease.Domains.Raffles.Model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static jakarta.persistence.FetchType.LAZY;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
@Entity
public class RaffleStatistics {
    @Id
    private Long id;

    @OneToOne(fetch = LAZY)
    @MapsId
    @JoinColumn(name = "raffle_id")
    private Raffle raffle;

    @Column(nullable = false)
    private Long availableTickets;

    @Column(nullable = false)
    private Long soldTickets;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal revenue;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal averageOrderValue;

    @Column(nullable = false)
    private Long totalOrders;

    @Column(nullable = false)
    private Long completedOrders;

    @Column(nullable = false)
    private Long pendingOrders;

    @Column(nullable = false)
    private Long cancelledOrders;

    @Column(nullable = false)
    private Long unpaidOrders;

    @Column(nullable = false)
    private Long refundedOrders;

    @Column(nullable = false)
    private Long participants;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal ticketsPerParticipant;

    private LocalDateTime firstSaleDate;
    private LocalDateTime lastSaleDate;
    private BigDecimal dailySalesVelocity;
}
