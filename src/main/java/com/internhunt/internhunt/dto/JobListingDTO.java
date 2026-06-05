package com.internhunt.internhunt.dto;

import com.internhunt.internhunt.entity.JobListing;

import java.time.LocalDateTime;

// FIX: was a class with all-public fields (no encapsulation, no validation possible).
//      Changed to a Java record — immutable, concise, Jackson-serialisable.
public record JobListingDTO(
        Integer       id,
        String        jobTitle,
        String        companyName,
        String        location,
        Boolean       isRemote,
        String        stipend,
        String        listingType,
        String        status,
        String        sourceUrl,
        String        sourceName,
        String        description,
        LocalDateTime scrapedAt,
        LocalDateTime postedAt,
        LocalDateTime deadline
)
{
    public static JobListingDTO from(JobListing job)
    {
        return new JobListingDTO(
                job.getId(),
                job.getJobTitle(),
                job.getCompanyName(),
                job.getLocation(),
                job.getIsRemote(),
                job.getStipend(),
                job.getListingType()  != null ? job.getListingType().name()  : null,
                job.getStatus()       != null ? job.getStatus().name()       : null,
                job.getSourceUrl(),
                job.getSource()       != null ? job.getSource().getName()    : null,
                job.getDescription(),
                job.getScrapedAt(),
                job.getPostedAt(),
                job.getDeadline()
        );
    }
}