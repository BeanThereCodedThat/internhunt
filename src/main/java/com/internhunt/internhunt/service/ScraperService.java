package com.internhunt.internhunt.service;

import com.internhunt.internhunt.entity.JobListing;
import com.internhunt.internhunt.entity.JobSource;
import com.internhunt.internhunt.entity.ScraperLog;
import com.internhunt.internhunt.entity.Source;
import com.internhunt.internhunt.repository.JobSourceRepository;
import com.internhunt.internhunt.repository.ScraperLogRepository;
import com.internhunt.internhunt.repository.SourceRepository;
import com.internhunt.internhunt.scraper.base.JobScraper;
import com.internhunt.internhunt.scraper.companies.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class ScraperService
{
    // ---- scrapers ----
    @Autowired private UnstopScraper          unstopScraper;
    @Autowired private InternshalaScraper      internshalaScraper;
    @Autowired private HackerNewsScraper       hackerNewsScraper;
    @Autowired private RedditScraper           redditScraper;
    @Autowired private CompanyCareersScrapers  companyCareersScrapers;
    @Autowired private LinkedInScraper         linkedInScraper;
    @Autowired private NaukriScraper           naukriScraper;
    @Autowired private IndeedScraper           indeedScraper;
    @Autowired private WellfoundScraper        wellfoundScraper;
    @Autowired private RssBlogScraper          rssBlogScraper;

    @Autowired private JobListingService    jobListingService;
    @Autowired private SourceRepository     sourceRepository;
    @Autowired private JobSourceRepository  jobSourceRepository;
    @Autowired private ScraperLogRepository scraperLogRepository;
    @Autowired private KeywordSkillExtractor skillExtractor;

    // Map scraper names to their instances
    private Map<String, JobScraper> scraperMap()
    {
        // Map.of() tops out at 10 key-value pairs — we're at exactly that limit,
        // so this uses Map.ofEntries() instead, which has no such ceiling.
        return Map.ofEntries(
            Map.entry("unstop",           unstopScraper),
            Map.entry("internshala",      internshalaScraper),
            Map.entry("hackernews",       hackerNewsScraper),
            Map.entry("reddit",           redditScraper),
            Map.entry("company_careers",  companyCareersScrapers),
            Map.entry("linkedin",         linkedInScraper),
            Map.entry("naukri",           naukriScraper),
            Map.entry("indeed",           indeedScraper),
            Map.entry("wellfound",        wellfoundScraper),
            Map.entry("rss_blogs",        rssBlogScraper)
        );
    }

    // ------------------------------------------------------------------ //
    //  Public API — all async so the HTTP response returns immediately     //
    // ------------------------------------------------------------------ //

    @Async
    public void runScraper(String sourceName)
    {
        JobScraper scraper = scraperMap().get(sourceName);
        if (scraper == null)
        {
            System.err.println("[scraper] Unknown source: " + sourceName);
            return;
        }
        runScraperInternal(sourceName, scraper);
    }

    /** Runs all active scrapers sequentially (used by the scheduler). */
    @Async
    public void runAllActive()
    {
        List<Source> activeSources = sourceRepository.findByIsActiveTrue();
        for (Source s : activeSources)
        {
            JobScraper scraper = scraperMap().get(s.getName());
            if (scraper != null) runScraperInternal(s.getName(), scraper);
        }
    }

    // Convenience individual methods (for ScraperController backward compat)
    @Async public void runUnstopScraper()         { runScraper("unstop");          }
    @Async public void runInternshalaScraper()    { runScraper("internshala");     }
    @Async public void runHackerNewsScraper()     { runScraper("hackernews");      }
    @Async public void runRedditScraper()         { runScraper("reddit");          }
    @Async public void runCompanyCareersScraper() { runScraper("company_careers"); }
    @Async public void runLinkedInScraper()       { runScraper("linkedin");        }
    @Async public void runNaukriScraper()         { runScraper("naukri");          }
    @Async public void runIndeedScraper()         { runScraper("indeed");          }
    @Async public void runWellfoundScraper()      { runScraper("wellfound");       }
    @Async public void runRssBlogScraper()        { runScraper("rss_blogs");       }

    // ------------------------------------------------------------------ //
    //  Shared scraper engine                                               //
    // ------------------------------------------------------------------ //

    private void runScraperInternal(String sourceName, JobScraper scraper)
    {
        Optional<Source> sourceOpt = sourceRepository.findByName(sourceName);

        if (sourceOpt.isEmpty())
        {
            System.err.println("[" + sourceName + "] Source not found in DB. Run the seed SQL first.");
            return;
        }

        Source source = sourceOpt.get();
        ScraperLog log = new ScraperLog();
        log.setSource(source);

        try
        {
            scraper.setSource(source);
            List<JobListing> jobs = scraper.scrape();

            int saved = 0, skipped = 0;

            for (JobListing job : jobs)
            {
                try
                {
                    Optional<JobSource> existing = jobSourceRepository.findBySourceUrl(job.getSourceUrl());
                    if (existing.isPresent()) { skipped++; continue; }

                    JobListing savedJob = jobListingService.createJobListing(job);

                    JobSource jobSource = new JobSource();
                    jobSource.setJob(savedJob);
                    jobSource.setSource(source);
                    jobSource.setSourceUrl(job.getSourceUrl());
                    jobSourceRepository.save(jobSource);

                    // Offline, keyword-based — no AI, no external calls. Fire-and-forget.
                    skillExtractor.extractSkillsForJob(savedJob);

                    saved++;
                }
                catch (Exception e)
                {
                    System.err.println("[" + sourceName + "] Failed to save job: " + e.getMessage());
                    skipped++;
                }
            }

            source.setLastScrapedAt(LocalDateTime.now());
            sourceRepository.save(source);

            log.setJobsFound(jobs.size());
            log.setJobsSaved(saved);
            log.setJobsSkipped(skipped);
            log.setStatus(ScraperLog.Status.SUCCESS);

            System.out.printf("[%s] done — found: %d  saved: %d  skipped: %d%n",
                    sourceName, jobs.size(), saved, skipped);
        }
        catch (Exception e)
        {
            log.setStatus(ScraperLog.Status.FAILED);
            log.setErrorMessage(e.getMessage());
            System.err.println("[" + sourceName + "] scraper failed: " + e.getMessage());
        }
        finally
        {
            scraperLogRepository.save(log);
        }
    }
}
