package com.internhunt.internhunt.repository;

import com.internhunt.internhunt.entity.ScamReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ScamReportRepository extends JpaRepository<ScamReport, Integer>
{
    List<ScamReport> findByCompanyNameContainingIgnoreCase(String companyName);
}