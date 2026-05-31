package com.internhunt.internhunt.service;

import com.internhunt.internhunt.entity.JobListing;
import com.internhunt.internhunt.entity.JobSource;
import com.internhunt.internhunt.entity.ScraperLog;
import com.internhunt.internhunt.entity.Source;
import com.internhunt.internhunt.repository.JobSourceRepository;
import com.internhunt.internhunt.repository.ScraperLogRepository;
import com.internhunt.internhunt.repository.SourceRepository;
import com.internhunt.internhunt.scraper.companies.InternshalaScraper;
import com.internhunt.internhunt.scraper.companies.UnstopScraper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class ScraperService
{
    @Autowired
    private UnstopScraper unstopScraper;

    @Autowired
    private InternshalaScraper internshalaScraper;

    @Autowired
    private JobListingService jobListingService;

    @Autowired
    private SourceRepository sourceRepository;

    @Autowired
    private JobSourceRepository jobSourceRepository;

    @Autowired
    private ScraperLogRepository scraperLogRepository;

    public void runScraper(String sourceName)
    {
        switch (sourceName)
        {
            case "unstop" -> runUnstopScraper();
            case "internshala" -> runInternshalaScraper();
            default -> System.err.println("Unknown scraper: " + sourceName);
        }
    }

    public void runUnstopScraper()
    {
        runScraperInternal("unstop", unstopScraper);
    }

    public void runInternshalaScraper()
    {
        runScraperInternal("internshala", internshalaScraper);
    }

    private void runScraperInternal(String sourceName,
                                    com.internhunt.internhunt.scraper.base.JobScraper scraper)
    {
        Optional<Source> sourceOpt = sourceRepository.findByName(sourceName);

        if (sourceOpt.isEmpty())
        {
            System.err.println(sourceName + " source not found in database");
            return;
        }

        Source source = sourceOpt.get();
        ScraperLog log = new ScraperLog();
        log.setSource(source);

        try
        {
            if (scraper instanceof UnstopScraper us) us.setSource(source);
            else if (scraper instanceof InternshalaScraper is) is.setSource(source);

            List<JobListing> jobs = scraper.scrape();

            int saved = 0;
            int skipped = 0;

            for (JobListing job : jobs)
            {
                try
                {
                    Optional<JobSource> existing = jobSourceRepository
                            .findBySourceUrl(job.getSourceUrl());

                    if (existing.isPresent())
                    {
                        skipped++;
                        continue;
                    }

                    JobListing savedJob = jobListingService.createJobListing(job);

                    JobSource jobSource = new JobSource();
                    jobSource.setJob(savedJob);
                    jobSource.setSource(source);
                    jobSource.setSourceUrl(job.getSourceUrl());
                    jobSourceRepository.save(jobSource);

                    saved++;
                }
                catch (Exception e)
                {
                    System.err.println("Failed to save job: " + e.getMessage());
                    skipped++;
                }
            }

            source.setLastScrapedAt(LocalDateTime.now());
            sourceRepository.save(source);

            log.setJobsFound(jobs.size());
            log.setJobsSaved(saved);
            log.setJobsSkipped(skipped);
            log.setStatus(ScraperLog.Status.SUCCESS);

            System.out.println(sourceName + " scrape complete — found: "
                    + jobs.size() + " saved: " + saved + " skipped: " + skipped);
        }
        catch (Exception e)
        {
            log.setStatus(ScraperLog.Status.FAILED);
            log.setErrorMessage(e.getMessage());
            System.err.println(sourceName + " scraper failed: " + e.getMessage());
        }
        finally
        {
            scraperLogRepository.save(log);
        }
    }
}