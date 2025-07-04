package com.raffleease.raffleease.Domains.Notifications.Repository;

import com.raffleease.raffleease.Domains.Notifications.Model.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface INotificationsRepository extends JpaRepository<Notification, Long> {
}
