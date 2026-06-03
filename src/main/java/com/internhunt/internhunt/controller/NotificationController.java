package com.internhunt.internhunt.controller;

import com.internhunt.internhunt.entity.Notification;
import com.internhunt.internhunt.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController
{
    @Autowired
    private NotificationService notificationService;

    @GetMapping("/user/{userId}")
    public List<Notification> getNotificationsByUser(@PathVariable Integer userId)
    {
        return notificationService.getNotificationsByUser(userId);
    }

    @GetMapping("/user/{userId}/unread")
    public List<Notification> getUnreadNotifications(@PathVariable Integer userId)
    {
        return notificationService.getUnreadNotifications(userId);
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<Notification> markAsRead(@PathVariable Integer id)
    {
        try
        {
            return ResponseEntity.ok(notificationService.markAsRead(id));
        }
        catch (RuntimeException e)
        {
            return ResponseEntity.notFound().build();
        }
    }
}