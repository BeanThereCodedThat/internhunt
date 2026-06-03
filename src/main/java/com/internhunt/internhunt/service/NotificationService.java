package com.internhunt.internhunt.service;

import com.internhunt.internhunt.entity.Notification;
import com.internhunt.internhunt.repository.NotificationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class NotificationService
{
    @Autowired
    private NotificationRepository notificationRepository;

    public Notification createNotification(Notification notification)
    {
        return notificationRepository.save(notification);
    }

    public List<Notification> getNotificationsByUser(Integer userId)
    {
        return notificationRepository.findByUserId(userId);
    }

    public List<Notification> getUnreadNotifications(Integer userId)
    {
        return notificationRepository.findByUserIdAndIsReadFalse(userId);
    }

    public Notification markAsRead(Integer id)
    {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Notification not found"));
        notification.setIsRead(true);
        return notificationRepository.save(notification);
    }
}