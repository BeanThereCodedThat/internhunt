package com.internhunt.internhunt.controller;

import com.internhunt.internhunt.service.ScraperService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/scraper")
public class ScraperController
{
    @Autowired
    private ScraperService scraperService;

    /** POST /api/scraper/run/{source} — trigger any single scraper by name */
    @PostMapping("/run/{source}")
    public ResponseEntity<?> runScraper(@PathVariable String source)
    {
        scraperService.runScraper(source);
        return ResponseEntity.accepted()
                .body(Map.of("message", source + " scraper started", "source", source));
    }

    /** POST /api/scraper/run-all — triggers all active scrapers */
    @PostMapping("/run-all")
    public ResponseEntity<?> runAll()
    {
        scraperService.runAllActive();
        return ResponseEntity.accepted()
                .body(Map.of("message", "All active scrapers started"));
    }

    // ── Named shortcuts (kept for frontend ScraperPanel backward compat) ── //

    @PostMapping("/unstop")
    public ResponseEntity<?> runUnstop()
    {
        scraperService.runUnstopScraper();
        return ResponseEntity.accepted().body(Map.of("message", "Unstop scraper started", "source", "unstop"));
    }

    @PostMapping("/internshala")
    public ResponseEntity<?> runInternshala()
    {
        scraperService.runInternshalaScraper();
        return ResponseEntity.accepted().body(Map.of("message", "Internshala scraper started", "source", "internshala"));
    }

    @PostMapping("/hackernews")
    public ResponseEntity<?> runHackerNews()
    {
        scraperService.runHackerNewsScraper();
        return ResponseEntity.accepted().body(Map.of("message", "HackerNews scraper started", "source", "hackernews"));
    }

    @PostMapping("/reddit")
    public ResponseEntity<?> runReddit()
    {
        scraperService.runRedditScraper();
        return ResponseEntity.accepted().body(Map.of("message", "Reddit scraper started", "source", "reddit"));
    }

    @PostMapping("/company-careers")
    public ResponseEntity<?> runCompanyCareers()
    {
        scraperService.runCompanyCareersScraper();
        return ResponseEntity.accepted().body(Map.of("message", "Company careers scraper started", "source", "company_careers"));
    }
}
