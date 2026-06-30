package com.internhunt.internhunt.repository;

import com.internhunt.internhunt.entity.JobSource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface JobSourceRepository extends JpaRepository<JobSource, Integer>
{
    Optional<JobSource> findBySourceUrl(String sourceUrl);

    List<JobSource> findByJobId(Integer jobId);

    // FIX: added to support batch deduplication in ScraperService
    //      instead of N individual findBySourceUrl calls in a loop
    @Query("SELECT js.sourceUrl FROM JobSource js")
    List<String> findAllSourceUrls();
}