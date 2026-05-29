package com.internhunt.internhunt.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "scraper_logs")
public class ScraperLog
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "source_id", nullable = false)
    private Source source;

    @Column(updatable = false)
    private LocalDateTime runAt;

    @Column(nullable = false)
    private Integer jobsFound = 0;

    @Column(nullable = false)
    private Integer jobsSaved = 0;

    @Column(nullable = false)
    private Integer jobsSkipped = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;

    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    @PrePersist
    protected void onCreate()
    {
        runAt = LocalDateTime.now();
    }

    public enum Status
    {
        SUCCESS,
        PARTIAL,
        FAILED
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

    public Source getSource()
    {
        return source;
    }

    public void setSource(Source source)
    {
        this.source = source;
    }

    public LocalDateTime getRunAt()
    {
        return runAt;
    }

    public void setRunAt(LocalDateTime runAt)
    {
        this.runAt = runAt;
    }

    public Integer getJobsFound()
    {
        return jobsFound;
    }

    public void setJobsFound(Integer jobsFound)
    {
        this.jobsFound = jobsFound;
    }

    public Integer getJobsSaved()
    {
        return jobsSaved;
    }

    public void setJobsSaved(Integer jobsSaved)
    {
        this.jobsSaved = jobsSaved;
    }

    public Integer getJobsSkipped()
    {
        return jobsSkipped;
    }

    public void setJobsSkipped(Integer jobsSkipped)
    {
        this.jobsSkipped = jobsSkipped;
    }

    public Status getStatus()
    {
        return status;
    }

    public void setStatus(Status status)
    {
        this.status = status;
    }

    public String getErrorMessage()
    {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage)
    {
        this.errorMessage = errorMessage;
    }
}