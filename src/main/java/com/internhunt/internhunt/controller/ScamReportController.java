package com.internhunt.internhunt.controller;

import com.internhunt.internhunt.entity.ScamReport;
import com.internhunt.internhunt.service.ScamReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

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

    /**
     * GET /api/scam-reports/check?company=CompanyName
     * Returns { flagged: true/false, severity, sourceUrl } for the frontend JobModal to display.
     */
    @GetMapping("/check")
    public ResponseEntity<?> checkCompany(@RequestParam String company)
    {
        List<ScamReport> reports = scamReportService.searchScamReports(company);

        if (reports.isEmpty())
        {
            return ResponseEntity.ok(Map.of("flagged", false));
        }

        ScamReport worst = reports.stream()
                .max(java.util.Comparator.comparing(r ->
                        r.getSeverity() == ScamReport.Severity.confirmed_scam ? 1 : 0))
                .orElse(reports.get(0));

        return ResponseEntity.ok(Map.of(
                "flagged",    true,
                "severity",   worst.getSeverity().name(),
                "sourceUrl",  worst.getSourceUrl(),
                "reportedAt", worst.getReportedAt() != null ? worst.getReportedAt().toString() : ""
        ));
    }

    @PostMapping
    public ScamReport createScamReport(@RequestBody ScamReport scamReport)
    {
        return scamReportService.createScamReport(scamReport);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteScamReport(@PathVariable Integer id)
    {
        scamReportRepository.deleteById(id);
        return ResponseEntity.ok().build();
    }

    @Autowired
    private com.internhunt.internhunt.repository.ScamReportRepository scamReportRepository;
}
