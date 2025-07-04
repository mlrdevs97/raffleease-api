package com.raffleease.raffleease.Domains.Raffles.Model;

import com.raffleease.raffleease.Domains.Associations.Model.Association;
import com.raffleease.raffleease.Domains.Images.Model.Image;
import com.raffleease.raffleease.Domains.Orders.Model.Order;
import com.raffleease.raffleease.Domains.Tickets.Model.Ticket;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static jakarta.persistence.CascadeType.ALL;
import static jakarta.persistence.CascadeType.PERSIST;
import static jakarta.persistence.EnumType.STRING;
import static jakarta.persistence.FetchType.LAZY;
import static jakarta.persistence.GenerationType.IDENTITY;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
@Entity
@Table(name = "Raffles")
public class Raffle {
    @Id
    @GeneratedValue(strategy = IDENTITY)
    private Long id;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "association_id", nullable = false)
    private Association association;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(STRING)
    @Column(nullable = false)
    private RaffleStatus status;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal ticketPrice;

    @Column(nullable = false)
    private Long totalTickets;

    @Column(nullable = false)
    private Long firstTicketNumber;

    @OneToMany(mappedBy = "raffle", cascade = ALL, orphanRemoval = true)
    private List<Image> images;

    @OneToMany(mappedBy = "raffle", cascade = ALL, orphanRemoval = true)
    private List<Ticket> tickets;

    @OneToMany(mappedBy = "raffle", cascade = PERSIST)
    private List<Order> orders;

    @Enumerated(STRING)
    private CompletionReason completionReason;

    @OneToOne(fetch = LAZY)
    @JoinColumn(name = "winning_ticket_id")
    private Ticket winningTicket;

    @OneToOne(mappedBy = "raffle", cascade = ALL, orphanRemoval = true, fetch = LAZY)
    private RaffleStatistics statistics;

    @Column(nullable = false, updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private LocalDateTime completedAt;
}