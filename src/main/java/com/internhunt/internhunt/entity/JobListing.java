package com.internhunt.internhunt.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "job_listings")
public class JobListing
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 200)
    private String companyName;

    @Column(nullable = false, length = 200)
    private String jobTitle;

    @Column(length = 100)
    private String stipend;

    @Column(length = 200)
    private String location;

    @Column(nullable = false)
    private Boolean isRemote = false;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ListingType listingType = ListingType.internship;

    @ManyToOne
    @JoinColumn(name = "source_id", nullable = false)
    private Source source;

    @Column(nullable = false, unique = true, length = 500)
    private String sourceUrl;

    private LocalDateTime postedAt;

    private LocalDateTime deadline;

    @Column(updatable = false)
    private LocalDateTime scrapedAt;

    @Column(nullable = false)
    private Boolean isExpired = false;

    @PrePersist
    protected void onCreate()
    {
        scrapedAt = LocalDateTime.now();
    }

    public enum ListingType
    {
        internship,
        full_time,
        contract
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

    public String getJobTitle()
    {
        return jobTitle;
    }

    public void setJobTitle(String jobTitle)
    {
        this.jobTitle = jobTitle;
    }

    public String getStipend()
    {
        return stipend;
    }

    public void setStipend(String stipend)
    {
        this.stipend = stipend;
    }

    public String getLocation()
    {
        return location;
    }

    public void setLocation(String location)
    {
        this.location = location;
    }

    public Boolean getIsRemote()
    {
        return isRemote;
    }

    public void setIsRemote(Boolean isRemote)
    {
        this.isRemote = isRemote;
    }

    public String getDescription()
    {
        return description;
    }

    public void setDescription(String description)
    {
        this.description = description;
    }

    public ListingType getListingType()
    {
        return listingType;
    }

    public void setListingType(ListingType listingType)
    {
        this.listingType = listingType;
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

    public LocalDateTime getPostedAt()
    {
        return postedAt;
    }

    public void setPostedAt(LocalDateTime postedAt)
    {
        this.postedAt = postedAt;
    }

    public LocalDateTime getDeadline()
    {
        return deadline;
    }

    public void setDeadline(LocalDateTime deadline)
    {
        this.deadline = deadline;
    }

    public LocalDateTime getScrapedAt()
    {
        return scrapedAt;
    }

    public void setScrapedAt(LocalDateTime scrapedAt)
    {
        this.scrapedAt = scrapedAt;
    }

    public Boolean getIsExpired()
    {
        return isExpired;
    }

    public void setIsExpired(Boolean isExpired)
    {
        this.isExpired = isExpired;
    }
}