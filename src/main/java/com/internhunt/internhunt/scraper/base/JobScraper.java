package com.internhunt.internhunt.scraper.base;

import com.internhunt.internhunt.entity.JobListing;
import java.util.List;

public interface JobScraper
{
    List<JobListing> scrape();

    String getSourceName();
}