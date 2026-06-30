package com.internhunt.internhunt.repository;

import com.internhunt.internhunt.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Integer>
{
    List<Notification> findByUserId(Integer userId);

    List<Notification> findByUserIdAndIsReadFalse(Integer userId);

    boolean existsByUserIdAndJobIdAndCreatedAtAfter(Integer userId, Integer jobId, LocalDateTime after);
}
