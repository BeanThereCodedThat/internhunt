package com.internhunt.internhunt.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "applications")
public class Application
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne
    @JoinColumn(name = "job_id", nullable = false)
    private JobListing job;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.pending;

    @Column(columnDefinition = "TEXT")
    private String coverLetter;

    @Column(updatable = false)
    private LocalDateTime appliedAt;

    @PrePersist
    protected void onCreate()
    {
        appliedAt = LocalDateTime.now();
    }

    public enum Status
    {
        pending,
        applied,
        rejected,
        selected
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

    public Status getStatus()
    {
        return status;
    }

    public void setStatus(Status status)
    {
        this.status = status;
    }

    public String getCoverLetter()
    {
        return coverLetter;
    }

    public void setCoverLetter(String coverLetter)
    {
        this.coverLetter = coverLetter;
    }

    public LocalDateTime getAppliedAt()
    {
        return appliedAt;
    }

    public void setAppliedAt(LocalDateTime appliedAt)
    {
        this.appliedAt = appliedAt;
    }
}