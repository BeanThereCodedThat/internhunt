package com.internhunt.internhunt.controller;

import com.internhunt.internhunt.dto.JobListingDTO;
import com.internhunt.internhunt.entity.JobListing;
import com.internhunt.internhunt.repository.JobListingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/jobs")
public class JobListingController
{
    @Autowired
    private JobListingRepository jobListingRepository;

    /**
     * GET /api/jobs
     * Params: page, size, search, source, type (internship/job), remote (true/false)
     */
    @GetMapping
    public ResponseEntity<?> getJobs(
            @RequestParam(defaultValue = "0")  int     page,
            @RequestParam(defaultValue = "20") int     size,
            @RequestParam(defaultValue = "")   String  search,
            @RequestParam(defaultValue = "")   String  source,
            @RequestParam(defaultValue = "")   String  type,
            @RequestParam(required = false)    Boolean remote)
    {
        size = Math.min(size, 50);

        // Convert type string to enum — null means "all types"
        JobListing.ListingType listingType = null;
        if (!type.isBlank())
        {
            try { listingType = JobListing.ListingType.valueOf(type.toUpperCase()); }
            catch (IllegalArgumentException ignored) {}
        }

        Page<JobListingDTO> results = jobListingRepository.search(
                search.isBlank() ? null : search,
                source.isBlank() ? null : source,
                listingType,
                remote,
                PageRequest.of(page, size)
        ).map(JobListingDTO::from);

        return ResponseEntity.ok(results);
    }

    /** GET /api/jobs/{id} */
    @GetMapping("/{id}")
    public ResponseEntity<?> getJob(@PathVariable Integer id)
    {
        return jobListingRepository.findById(id)
                .map(job -> ResponseEntity.ok(JobListingDTO.from(job)))
                .orElse(ResponseEntity.notFound().build());
    }
}