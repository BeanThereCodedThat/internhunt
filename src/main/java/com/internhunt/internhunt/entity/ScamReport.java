package com.internhunt.internhunt.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "scam_reports")
public class ScamReport
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 200)
    private String companyName;

    @Column(nullable = false, length = 500)
    private String sourceUrl;

    @Column(updatable = false)
    private LocalDateTime reportedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Severity severity;

    @PrePersist
    protected void onCreate()
    {
        reportedAt = LocalDateTime.now();
    }

    public enum Severity
    {
        warning,
        confirmed_scam
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

    public String getCompanyName()
    {
        return companyName;
    }

    public void setCompanyName(String companyName)
    {
        this.companyName = companyName;
    }

    public String getSourceUrl()
    {
        return sourceUrl;
    }

    public void setSourceUrl(String sourceUrl)
    {
        this.sourceUrl = sourceUrl;
    }

    public LocalDateTime getReportedAt()
    {
        return reportedAt;
    }

    public void setReportedAt(LocalDateTime reportedAt)
    {
        this.reportedAt = reportedAt;
    }

    public Severity getSeverity()
    {
        return severity;
    }

    public void setSeverity(Severity severity)
    {
        this.severity = severity;
    }
}