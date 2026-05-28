package com.internhunt.internhunt.service;

import com.internhunt.internhunt.entity.ScamReport;
import com.internhunt.internhunt.repository.ScamReportRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class ScamReportService
{
    @Autowired
    private ScamReportRepository scamReportRepository;

    public ScamReport createScamReport(ScamReport scamReport)
    {
        return scamReportRepository.save(scamReport);
    }

    public List<ScamReport> getAllScamReports()
    {
        return scamReportRepository.findAll();
    }

    public List<ScamReport> searchScamReports(String companyName)
    {
        return scamReportRepository.findByCompanyNameContainingIgnoreCase(companyName);
    }

    public boolean isCompanyFlagged(String companyName)
    {
        return !scamReportRepository.findByCompanyNameContainingIgnoreCase(companyName).isEmpty();
    }
}