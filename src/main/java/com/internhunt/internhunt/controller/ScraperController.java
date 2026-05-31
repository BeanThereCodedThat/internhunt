package com.internhunt.internhunt.controller;

import com.internhunt.internhunt.service.ScraperService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
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

    @PostMapping("/internshala")
    public String runInternshalaScraper()
    {
        scraperService.runInternshalaScraper();
        return "Internshala scraper started";
    }

    @PostMapping("/run/{source}")
    public String runScraper(@PathVariable String source)
    {
        scraperService.runScraper(source);
        return source + " scraper started";
    }
}