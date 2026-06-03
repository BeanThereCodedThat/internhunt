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

    @PostMapping("/unstop")
    public ResponseEntity<?> runUnstop()
    {
        scraperService.runUnstopScraper();
        return ResponseEntity.accepted()
                .body(Map.of("message", "Unstop scraper started", "source", "unstop"));
    }

    @PostMapping("/internshala")
    public ResponseEntity<?> runInternshala()
    {
        scraperService.runInternshalaScraper();
        return ResponseEntity.accepted()
                .body(Map.of("message", "Internshala scraper started", "source", "internshala"));
    }

    @PostMapping("/hackernews")
    public ResponseEntity<?> runHackerNews()
    {
        scraperService.runHackerNewsScraper();
        return ResponseEntity.accepted()
                .body(Map.of("message", "HackerNews scraper started", "source", "hackernews"));
    }

    @PostMapping("/run/{source}")
    public ResponseEntity<?> runScraper(@PathVariable String source)
    {
        scraperService.runScraper(source);
        return ResponseEntity.accepted()
                .body(Map.of("message", source + " scraper started", "source", source));
    }
}
