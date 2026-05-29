package com.internhunt.internhunt.controller;

import com.internhunt.internhunt.entity.JobListing;
import com.internhunt.internhunt.service.JobListingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/jobs")
public class JobListingController
{
    @Autowired
    private JobListingService jobListingService;

    @GetMapping
    public List<JobListing> getAllJobs()
    {
        return jobListingService.getAllJobListings();
    }

    @GetMapping("/active")
    public List<JobListing> getActiveJobs()
    {
        return jobListingService.getActiveJobListings();
    }

    @GetMapping("/remote")
    public List<JobListing> getRemoteJobs()
    {
        return jobListingService.getRemoteJobListings();
    }

    @GetMapping("/search")
    public List<JobListing> searchJobs(@RequestParam String keyword)
    {
        return jobListingService.searchByCompany(keyword);
    }

    @GetMapping("/{id}")
    public ResponseEntity<JobListing> getJobById(@PathVariable Integer id)
    {
        return jobListingService.getJobListingById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public JobListing createJob(@RequestBody JobListing jobListing)
    {
        return jobListingService.createJobListing(jobListing);
    }

    @PutMapping("/{id}")
    public ResponseEntity<JobListing> updateJob(@PathVariable Integer id, @RequestBody JobListing jobListing)
    {
        return jobListingService.getJobListingById(id)
                .map(existing ->
                {
                    jobListing.setId(id);
                    return ResponseEntity.ok(jobListingService.updateJobListing(jobListing));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteJob(@PathVariable Integer id)
    {
        return jobListingService.getJobListingById(id)
                .map(job ->
                {
                    jobListingService.deleteJobListing(id);
                    return ResponseEntity.ok().<Void>build();
                })
                .orElse(ResponseEntity.notFound().build());
    }
}