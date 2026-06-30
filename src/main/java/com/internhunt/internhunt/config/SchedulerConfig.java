package com.internhunt.internhunt.config;

import com.internhunt.internhunt.entity.JobListing;
import com.internhunt.internhunt.entity.Notification;
import com.internhunt.internhunt.entity.User;
import com.internhunt.internhunt.repository.JobListingRepository;
import com.internhunt.internhunt.repository.NotificationRepository;
import com.internhunt.internhunt.repository.UserRepository;
import com.internhunt.internhunt.service.ScraperService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Phase 1B: Scheduled Tasks
 *
 * 1. Auto-scraping every 12 hours (Unstop + Internshala + HackerNews + Reddit + Company careers + RSS blogs)
 * 2. Deadline notifications: every morning, scan for jobs closing in ≤ 3 days
 * 3. Expired job cleanup: mark ACTIVE jobs past deadline as EXPIRED
 */
@Configuration
@EnableScheduling
public class SchedulerConfig
{
    @Autowired private ScraperService scraperService;
    @Autowired private JobListingRepository jobListingRepository;
    @Autowired private NotificationRepository notificationRepository;
    @Autowired private UserRepository userRepository;

    /**
     * Run core scrapers every 12 hours.
     * cron: sec min hour day month weekday
     * → runs at 6 AM and 6 PM every day
     */
    @Scheduled(cron = "0 0 6,18 * * *")
    public void runCoreScrapeSchedule()
    {
        System.out.println("[scheduler] Starting scheduled scrape at " + now());

        // Phase 1A: Company career pages
        scraperService.runScraper("company_careers");

        // Phase 1B: Public listing platforms
        scraperService.runScraper("unstop");
        scraperService.runScraper("internshala");
        scraperService.runScraper("hackernews");
        scraperService.runScraper("reddit");

        // RSS engineering blogs — filtered for hiring-related posts only
        scraperService.runScraper("rss_blogs");

        System.out.println("[scheduler] All scrapers dispatched.");
    }

    /**
     * Daily deadline notification at 8 AM.
     * Creates a notification for every user for jobs closing within 3 days.
     */
    @Scheduled(cron = "0 0 8 * * *")
    public void generateDeadlineNotifications()
    {
        System.out.println("[scheduler] Checking deadlines at " + now());

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime soon = now.plusDays(3);

        List<JobListing> closingSoon = jobListingRepository
                .findByStatusAndDeadlineBetween(JobListing.Status.ACTIVE, now, soon);

        if (closingSoon.isEmpty())
        {
            System.out.println("[scheduler] No jobs closing in next 3 days.");
            return;
        }

        List<User> users = userRepository.findAll();
        if (users.isEmpty())
        {
            System.out.println("[scheduler] No users to notify.");
            return;
        }

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd MMM yyyy");
        int notifCount = 0;

        for (User user : users)
        {
            for (JobListing job : closingSoon)
            {
                // Don't spam — only create if no notification for this (user, job) already exists today
                boolean alreadyNotified = notificationRepository
                        .existsByUserIdAndJobIdAndCreatedAtAfter(user.getId(), job.getId(), now.minusHours(20));
                if (alreadyNotified) continue;

                long daysLeft = java.time.temporal.ChronoUnit.DAYS.between(now, job.getDeadline());
                String urgency = daysLeft == 0 ? "TODAY" : "in " + daysLeft + " day" + (daysLeft > 1 ? "s" : "");

                String message = String.format(
                        "⏰ Deadline %s: %s at %s — closes %s",
                        urgency,
                        job.getJobTitle(),
                        job.getCompanyName(),
                        job.getDeadline().format(fmt)
                );

                Notification notification = new Notification();
                notification.setUser(user);
                notification.setJob(job);
                notification.setMessage(message);
                notificationRepository.save(notification);
                notifCount++;
            }
        }

        System.out.println("[scheduler] Created " + notifCount + " deadline notifications.");
    }

    /**
     * Daily at midnight: mark ACTIVE jobs where deadline has passed as EXPIRED.
     */
    @Scheduled(cron = "0 0 0 * * *")
    public void expireOldListings()
    {
        System.out.println("[scheduler] Expiring old listings at " + now());

        LocalDateTime now = LocalDateTime.now();
        List<JobListing> expired = jobListingRepository
                .findByStatusAndDeadlineBefore(JobListing.Status.ACTIVE, now);

        int count = 0;
        for (JobListing job : expired)
        {
            job.setStatus(JobListing.Status.EXPIRED);
            jobListingRepository.save(job);
            count++;
        }

        System.out.println("[scheduler] Marked " + count + " listings as EXPIRED.");
    }

    private String now()
    {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }
}
