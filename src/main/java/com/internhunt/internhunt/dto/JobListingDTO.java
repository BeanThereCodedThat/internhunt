package com.internhunt.internhunt.dto;

import com.internhunt.internhunt.entity.JobListing;

import java.time.LocalDateTime;

public class JobListingDTO
{
    public Integer       id;
    public String        jobTitle;
    public String        companyName;
    public String        location;
    public Boolean       isRemote;
    public String        stipend;
    public String        listingType;
    public String        status;
    public String        sourceUrl;
    public String        sourceName;
    public String        description;
    public LocalDateTime scrapedAt;
    public LocalDateTime postedAt;
    public LocalDateTime deadline;

    public static JobListingDTO from(JobListing job)
    {
        JobListingDTO dto = new JobListingDTO();
        dto.id          = job.getId();
        dto.jobTitle    = job.getJobTitle();
        dto.companyName = job.getCompanyName();
        dto.location    = job.getLocation();
        dto.isRemote    = job.getIsRemote();
        dto.stipend     = job.getStipend();
        dto.listingType = job.getListingType() != null
                ? job.getListingType().name() : null;
        dto.status      = job.getStatus() != null
                ? job.getStatus().name() : null;
        dto.sourceUrl   = job.getSourceUrl();
        dto.sourceName  = job.getSource() != null
                ? job.getSource().getName() : null;
        dto.description = job.getDescription();
        dto.scrapedAt   = job.getScrapedAt();
        dto.postedAt    = job.getPostedAt();
        dto.deadline    = job.getDeadline();
        return dto;
    }
}