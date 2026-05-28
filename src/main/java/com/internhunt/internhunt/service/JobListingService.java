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
        return jobListingRepository.findByIsExpiredFalse();
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