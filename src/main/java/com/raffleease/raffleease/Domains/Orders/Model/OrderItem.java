package com.raffleease.raffleease.Domains.Orders.Model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

import static jakarta.persistence.GenerationType.IDENTITY;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
@Entity
@Table(name = "Order_items")
public class OrderItem {
    @Id
    @GeneratedValue(strategy = IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String ticketNumber;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal priceAtPurchase;

    @Column(nullable = false, updatable = false)
    private Long ticketId;

    @Column(nullable = false, updatable = false)
    private Long raffleId;

    @Column(nullable = false, updatable = false)
    private Long customerId;

    @ManyToOne
    @JoinColumn(name = "order_id")
    private Order order;
}