package com.internhunt.internhunt.service;

import com.internhunt.internhunt.entity.JobListing;
import com.internhunt.internhunt.repository.JobListingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
public class JobListingService
{
    @Autowired
    private JobListingRepository jobListingRepository;

    public JobListing createJobListing(JobListing jobListing)
    {
        return jobListingRepository.save(jobListing);
    }

    public Optional<JobListing> getJobListingById(Integer id)
    {
        return jobListingRepository.findById(id);
    }

    public List<JobListing> getAllJobListings()
    {
        return jobListingRepository.findAll();
    }

    public List<JobListing> getActiveJobListings()
    {
        return jobListingRepository.findByStatus(JobListing.Status.ACTIVE);
    }

    public List<JobListing> getActiveRemoteJobListings()
    {
        return jobListingRepository.findByStatusAndIsRemoteTrue(JobListing.Status.ACTIVE);
    }

    public JobListing updateStatus(Integer id, JobListing.Status status)
    {
        return jobListingRepository.findById(id)
                .map(job ->
                {
                    job.setStatus(status);
                    job.setLastCheckedAt(java.time.LocalDateTime.now());
                    return jobListingRepository.save(job);
                })
                .orElseThrow(() -> new RuntimeException("Job not found"));
    }

    public List<JobListing> getRemoteJobListings()
    {
        return jobListingRepository.findByIsRemoteTrue();
    }

    public List<JobListing> searchByCompany(String keyword)
    {
        return jobListingRepository.findByCompanyNameContainingIgnoreCase(keyword);
    }

    public JobListing updateJobListing(JobListing jobListing)
    {
        return jobListingRepository.save(jobListing);
    }

    public void deleteJobListing(Integer id)
    {
        jobListingRepository.deleteById(id);
    }
}