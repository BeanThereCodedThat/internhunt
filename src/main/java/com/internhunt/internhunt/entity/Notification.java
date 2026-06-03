package com.internhunt.internhunt.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
public class Notification
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne
    @JoinColumn(name = "job_id")
    private JobListing job;

    @Column(nullable = false, length = 500)
    private String message;

    @Column(nullable = false)
    private Boolean isRead = false;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate()
    {
        createdAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Integer getId()
    {
        return id;
    }

    public void setId(Integer id)
    {
        this.id = id;
    }

    public User getUser()
    {
        return user;
    }

    public void setUser(User user)
    {
        this.user = user;
    }

    public JobListing getJob()
    {
        return job;
    }

    public void setJob(JobListing job)
    {
        this.job = job;
    }

    public String getMessage()
    {
        return message;
    }

    public void setMessage(String message)
    {
        this.message = message;
    }

    public Boolean getIsRead()
    {
        return isRead;
    }

    public void setIsRead(Boolean isRead)
    {
        this.isRead = isRead;
    }

    public LocalDateTime getCreatedAt()
    {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt)
    {
        this.createdAt = createdAt;
    }
}