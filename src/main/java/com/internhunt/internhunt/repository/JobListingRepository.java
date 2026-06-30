package com.internhunt.internhunt.repository;

import com.internhunt.internhunt.entity.JobListing;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface JobListingRepository extends JpaRepository<JobListing, Integer>
{
    // ── Used by ScraperService / JobListingService ──────────────────────── //
    List<JobListing> findByStatus(JobListing.Status status);
    List<JobListing> findByStatusAndIsRemoteTrue(JobListing.Status status);
    List<JobListing> findByIsRemoteTrue();
    List<JobListing> findByCompanyNameContainingIgnoreCase(String company);

    // ── Deadline tracking (used by SchedulerConfig) ─────────────────────── //
    List<JobListing> findByStatusAndDeadlineBetween(
            JobListing.Status status, LocalDateTime from, LocalDateTime to);

    List<JobListing> findByStatusAndDeadlineBefore(
            JobListing.Status status, LocalDateTime before);

    // ── Search with filters (used by JobListingController) ──────────────── //
    @Query("""
        SELECT j FROM JobListing j
        WHERE (:search IS NULL
               OR LOWER(j.jobTitle)    LIKE LOWER(CONCAT('%',:search,'%'))
               OR LOWER(j.companyName) LIKE LOWER(CONCAT('%',:search,'%'))
               OR LOWER(j.description) LIKE LOWER(CONCAT('%',:search,'%')))
          AND (:source IS NULL OR LOWER(j.source.name) = LOWER(:source))
          AND (:type   IS NULL OR j.listingType = :type)
          AND (:remote IS NULL OR j.isRemote    = :remote)
        ORDER BY j.scrapedAt DESC
        """)
    Page<JobListing> search(
            @Param("search") String search,
            @Param("source") String source,
            @Param("type")   JobListing.ListingType type,
            @Param("remote") Boolean remote,
            Pageable pageable
    );

    // ── Stats (used by StatsController) ─────────────────────────────────── //
    long countByListingType(JobListing.ListingType type);
    long countByIsRemoteTrue();
    long countByScrapedAtAfter(LocalDateTime since);

    @Query("SELECT j.source.name, COUNT(j) FROM JobListing j GROUP BY j.source.name")
    List<Object[]> countBySource();
}
