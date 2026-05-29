package com.internhunt.internhunt.repository;

import com.internhunt.internhunt.entity.JobListing;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface JobListingRepository extends JpaRepository<JobListing, Integer>
{
    List<JobListing> findByStatus(JobListing.Status status);

    List<JobListing> findByStatusAndIsRemoteTrue(JobListing.Status status);

    List<JobListing> findByCompanyNameContainingIgnoreCase(String keyword);

    List<JobListing> findByIsRemoteTrue();

    List<JobListing> findBySourceId(Integer sourceId);
}