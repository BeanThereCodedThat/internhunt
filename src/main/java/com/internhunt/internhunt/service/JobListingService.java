package com.internhunt.internhunt.service;

import com.internhunt.internhunt.entity.JobListing;
import com.internhunt.internhunt.repository.JobListingRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class JobListingService
{
    // FIX: constructor injection
    private final JobListingRepository jobListingRepository;

    public JobListingService(JobListingRepository jobListingRepository)
    {
        this.jobListingRepository = jobListingRepository;
    }

    /** Called by ScraperService to persist each scraped job. */
    public JobListing createJobListing(JobListing job)
    {
        if (job.getStatus() == null)
            job.setStatus(JobListing.Status.ACTIVE);
        if (job.getLastSeenAt() == null)
            job.setLastSeenAt(LocalDateTime.now());
        return jobListingRepository.save(job);
    }

    public Optional<JobListing> getById(Integer id)
    {
        return jobListingRepository.findById(id);
    }

    public List<JobListing> getActiveJobs()
    {
        return jobListingRepository.findByStatus(JobListing.Status.ACTIVE);
    }

    public List<JobListing> getActiveRemoteJobs()
    {
        return jobListingRepository.findByStatusAndIsRemoteTrue(JobListing.Status.ACTIVE);
    }

    public List<JobListing> getRemoteJobs()
    {
        return jobListingRepository.findByIsRemoteTrue();
    }

    public List<JobListing> searchByCompany(String company)
    {
        return jobListingRepository.findByCompanyNameContainingIgnoreCase(company);
    }

    public long getTotalCount()
    {
        return jobListingRepository.count();
    }
}