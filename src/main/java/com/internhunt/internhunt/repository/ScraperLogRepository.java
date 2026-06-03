package com.internhunt.internhunt.repository;

import com.internhunt.internhunt.entity.ScraperLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ScraperLogRepository extends JpaRepository<ScraperLog, Integer>
{
    List<ScraperLog> findBySourceId(Integer sourceId);

    List<ScraperLog> findByStatus(ScraperLog.Status status);
}