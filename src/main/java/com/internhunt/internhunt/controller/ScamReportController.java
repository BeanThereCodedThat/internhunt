package com.internhunt.internhunt.controller;

import com.internhunt.internhunt.entity.ScamReport;
import com.internhunt.internhunt.service.ScamReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/scam-reports")
public class ScamReportController
{
    @Autowired
    private ScamReportService scamReportService;

    @GetMapping
    public List<ScamReport> getAllScamReports()
    {
        return scamReportService.getAllScamReports();
    }

    @GetMapping("/search")
    public List<ScamReport> searchScamReports(@RequestParam String company)
    {
        return scamReportService.searchScamReports(company);
    }

    @GetMapping("/check")
    public boolean isCompanyFlagged(@RequestParam String company)
    {
        return scamReportService.isCompanyFlagged(company);
    }

    @PostMapping
    public ScamReport createScamReport(@RequestBody ScamReport scamReport)
    {
        return scamReportService.createScamReport(scamReport);
    }
}