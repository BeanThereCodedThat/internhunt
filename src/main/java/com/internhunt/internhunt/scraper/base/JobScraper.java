package com.internhunt.internhunt.scraper.base;

import com.internhunt.internhunt.entity.JobListing;
import com.internhunt.internhunt.entity.Source;

import java.util.List;

public interface JobScraper
{
    String getSourceName();
    List<JobListing> scrape();
    void setSource(Source source);
}