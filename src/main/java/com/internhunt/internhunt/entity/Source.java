package com.internhunt.internhunt.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "sources")
public class Source
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(nullable = false, length = 500)
    private String url;

    @Column(nullable = false)
    private Integer scrapeFrequency = 24;

    private LocalDateTime lastScrapedAt;

    @Column(nullable = false)
    private Boolean isActive = true;

    // Getters and Setters
    public Integer getId()
    {
        return id;
    }

    public void setId(Integer id)
    {
        this.id = id;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public String getUrl()
    {
        return url;
    }

    public void setUrl(String url)
    {
        this.url = url;
    }

    public Integer getScrapeFrequency()
    {
        return scrapeFrequency;
    }

    public void setScrapeFrequency(Integer scrapeFrequency)
    {
        this.scrapeFrequency = scrapeFrequency;
    }

    public LocalDateTime getLastScrapedAt()
    {
        return lastScrapedAt;
    }

    public void setLastScrapedAt(LocalDateTime lastScrapedAt)
    {
        this.lastScrapedAt = lastScrapedAt;
    }

    public Boolean getIsActive()
    {
        return isActive;
    }

    public void setIsActive(Boolean isActive)
    {
        this.isActive = isActive;
    }
}