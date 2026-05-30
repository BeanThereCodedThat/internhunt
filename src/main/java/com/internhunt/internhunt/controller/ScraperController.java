package com.internhunt.internhunt.controller;

import com.internhunt.internhunt.service.ScraperService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/scraper")
public class ScraperController
{
    @Autowired
    private ScraperService scraperService;

    @PostMapping("/unstop")
    public String runUnstopScraper()
    {
        scraperService.runUnstopScraper();
        return "Unstop scraper started";
    }
}