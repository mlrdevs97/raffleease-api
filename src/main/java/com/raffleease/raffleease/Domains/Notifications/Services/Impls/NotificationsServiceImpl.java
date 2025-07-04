package com.raffleease.raffleease.Domains.Notifications.Services.Impls;

import com.raffleease.raffleease.Domains.Notifications.Model.Notification;
import com.raffleease.raffleease.Domains.Notifications.Model.NotificationChannel;
import com.raffleease.raffleease.Domains.Notifications.Model.NotificationType;
import com.raffleease.raffleease.Domains.Notifications.Repository.INotificationsRepository;
import com.raffleease.raffleease.Domains.Notifications.Services.NotificationsService;
import com.raffleease.raffleease.Common.Exceptions.CustomExceptions.DatabaseException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class NotificationsServiceImpl implements NotificationsService {
    private final INotificationsRepository repository;

    @Override
    public Notification create(NotificationType notificationType, NotificationChannel channel) {
        try {
            return repository.save(Notification.builder()
                    .channel(channel)
                    .notificationType(notificationType)
                    .build());
        } catch (DataAccessException ex) {
            throw new DatabaseException("Database error occurred while saving notification: " + ex.getMessage());
        }
    }
}
