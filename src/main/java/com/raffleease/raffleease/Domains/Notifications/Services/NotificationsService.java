package com.raffleease.raffleease.Domains.Notifications.Services;

import com.raffleease.raffleease.Domains.Notifications.Model.Notification;
import com.raffleease.raffleease.Domains.Notifications.Model.NotificationChannel;
import com.raffleease.raffleease.Domains.Notifications.Model.NotificationType;

public interface NotificationsService {
    /**
     * Creates a new notification entity in the database.
     * 
     * @param notificationType the type of notification
     * @param channel the channel of the notification
     * @return the created notification entity
     */
    Notification create(NotificationType notificationType, NotificationChannel channel);
}
