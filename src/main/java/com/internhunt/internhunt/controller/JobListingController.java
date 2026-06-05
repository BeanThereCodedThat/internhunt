package com.internhunt.internhunt.controller;

import com.internhunt.internhunt.dto.JobListingDTO;
import com.internhunt.internhunt.entity.JobListing;
import com.internhunt.internhunt.repository.JobListingRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/jobs")
public class JobListingController
{
    // FIX: constructor injection instead of @Autowired field injection
    private final JobListingRepository jobListingRepository;

    public JobListingController(JobListingRepository jobListingRepository)
    {
        this.jobListingRepository = jobListingRepository;
    }

    /**
     * GET /api/jobs
     * Params: page, size, search, source, type, remote, paid
     *
     * paid=true  → only listings with a non-null / non-"Unpaid" stipend
     * paid=false → only unpaid / null-stipend listings
     * (omit)     → no stipend filter
     */
    @GetMapping
    public ResponseEntity<?> getJobs(
            @RequestParam(defaultValue = "0")  int     page,
            @RequestParam(defaultValue = "20") int     size,
            @RequestParam(defaultValue = "")   String  search,
            @RequestParam(defaultValue = "")   String  source,
            @RequestParam(defaultValue = "")   String  type,
            @RequestParam(required = false)    Boolean remote,
            @RequestParam(required = false)    Boolean paid)
    {
        size = Math.min(size, 50);

        JobListing.ListingType listingType = null;
        if (!type.isBlank())
        {
            try
            {
                listingType = JobListing.ListingType.valueOf(type.toLowerCase());
            }
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