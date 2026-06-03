package com.internhunt.internhunt.controller;

import com.internhunt.internhunt.dto.StatsDTO;
import com.internhunt.internhunt.entity.JobListing;
import com.internhunt.internhunt.repository.JobListingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/stats")
public class StatsController
{
    @Autowired
    private JobListingRepository jobListingRepository;

    @GetMapping
    public StatsDTO getStats()
    {
        StatsDTO stats = new StatsDTO();

        stats.totalJobs        = jobListingRepository.countByListingType(JobListing.ListingType.full_time);
        stats.totalInternships = jobListingRepository.countByListingType(JobListing.ListingType.internship);
        stats.totalRemote      = jobListingRepository.countByIsRemoteTrue();
        stats.savedToday       = jobListingRepository.countByScrapedAtAfter(
                LocalDateTime.now().minusHours(24));

        List<Object[]> raw = jobListingRepository.countBySource();
        stats.bySource = raw.stream()
                .map(row -> new StatsDTO.SourceCount((String) row[0], (Long) row[1]))
                .toList();

        return stats;
    }
}