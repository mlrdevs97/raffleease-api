package com.raffleease.raffleease.Domains.Notifications.Model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

import static jakarta.persistence.EnumType.STRING;
import static jakarta.persistence.GenerationType.IDENTITY;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
@Entity
public class Notification {
    @Id
    @GeneratedValue(strategy = IDENTITY)
    private Long id;

    @Enumerated(STRING)
    @Column(nullable = false, updatable = false)
    private NotificationType notificationType;

    @Enumerated(STRING)
    @Column(nullable = false, updatable = false)
    private NotificationChannel channel;

    @CreationTimestamp
    @Column(updatable = false, nullable = false)
    private LocalDateTime notificationDate;
}