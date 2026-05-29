package com.internhunt.internhunt.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "job_sources")
public class JobSource
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "job_id", nullable = false)
    private JobListing job;

    @ManyToOne
    @JoinColumn(name = "source_id", nullable = false)
    private Source source;

    @Column(nullable = false, unique = true, length = 500)
    private String sourceUrl;

    @Column(updatable = false)
    private LocalDateTime foundAt;

    @PrePersist
    protected void onCreate()
    {
        foundAt = LocalDateTime.now();
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

    public JobListing getJob()
    {
        return job;
    }

    public void setJob(JobListing job)
    {
        this.job = job;
    }

    public Source getSource()
    {
        return source;
    }

    public void setSource(Source source)
    {
        this.source = source;
    }

    public String getSourceUrl()
    {
        return sourceUrl;
    }

    public void setSourceUrl(String sourceUrl)
    {
        this.sourceUrl = sourceUrl;
    }

    public LocalDateTime getFoundAt()
    {
        return foundAt;
    }

    public void setFoundAt(LocalDateTime foundAt)
    {
        this.foundAt = foundAt;
    }
}