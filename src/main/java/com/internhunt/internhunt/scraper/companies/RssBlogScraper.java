package com.internhunt.internhunt.scraper.companies;

import com.internhunt.internhunt.entity.JobListing;
import com.internhunt.internhunt.entity.Source;
import com.internhunt.internhunt.scraper.base.JobScraper;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * RssBlogScraper — scans engineering-blog RSS feeds for hiring-relevant posts.
 *
 * These are blogs, not job boards, so most posts aren't job listings at all.
 * Rather than ingesting every post as a fake "job" (which would pollute the
 * dashboard with irrelevant content), this filters each entry's title +
 * summary against a small set of hiring keywords — the same pattern Reddit's
 * scraper already uses for its "who's hiring" threads. Anything that doesn't
 * match is skipped, not saved.
 *
 * Each match becomes a JobListing pointing back at the blog post itself
 * (companies that post "We're Hiring" on their eng blog almost always link
 * to the actual posting from there).
 */
@Component
public class RssBlogScraper implements JobScraper
{
    // name -> RSS feed URL. All public, no auth required.
    private static final Map<String, String> FEEDS = Map.ofEntries(
            Map.entry("Netflix Tech Blog",     "https://netflixtechblog.com/feed"),
            Map.entry("Uber Engineering",      "https://www.uber.com/blog/engineering/rss/"),
            Map.entry("Airbnb Engineering",    "https://medium.com/feed/airbnb-engineering"),
            Map.entry("Spotify Engineering",   "https://engineering.atspotify.com/feed/"),
            Map.entry("Stripe Engineering",    "https://stripe.com/blog/engineering/feed.rss"),
            Map.entry("GitHub Blog",           "https://github.blog/feed/"),
            Map.entry("Razorpay Engineering",  "https://engineering.razorpay.com/feed"),
            Map.entry("Swiggy Bytes",          "https://bytes.swiggy.com/feed")
    );

    private static final String[] HIRING_KEYWORDS = {
            "we're hiring", "we are hiring", "join our team", "join us",
            "open position", "open role", "now hiring", "career opportunit",
            "internship", "intern program", "we're growing our team"
    };

    private Source source;

    @Override
    public void setSource(Source source) { this.source = source; }

    @Override
    public String getSourceName() { return "rss_blogs"; }

    @Override
    public List<JobListing> scrape()
    {
        List<JobListing> jobs = new ArrayList<>();

        for (Map.Entry<String, String> feed : FEEDS.entrySet())
        {
            String blogName = feed.getKey();
            String feedUrl  = feed.getValue();

            try
            {
                List<SyndEntry> entries = fetchEntries(feedUrl);
                System.out.println("[rss_blogs] " + blogName + " — " + entries.size() + " posts checked");

                for (SyndEntry entry : entries)
                {
                    JobListing job = toJobListingIfHiringRelated(entry, blogName);
                    if (job != null)
                    {
                        jobs.add(job);
                    }
                }
            }
            catch (Exception e)
            {
                // One feed failing (dead URL, temporary outage) shouldn't take down the others.
                System.err.println("[rss_blogs] " + blogName + " failed: " + e.getMessage());
            }
        }

        System.out.println("[rss_blogs] total hiring-related posts found: " + jobs.size());
        return jobs;
    }

    private List<SyndEntry> fetchEntries(String feedUrl) throws Exception
    {
        URL url = new URL(feedUrl);
        SyndFeedInput input = new SyndFeedInput();
        try (XmlReader reader = new XmlReader(url))
        {
            SyndFeed feed = input.build(reader);
            return feed.getEntries();
        }
    }

    private JobListing toJobListingIfHiringRelated(SyndEntry entry, String blogName)
    {
        String title   = entry.getTitle() == null ? "" : entry.getTitle();
        String summary = entry.getDescription() != null ? entry.getDescription().getValue() : "";
        String combined = (title + " " + summary).toLowerCase(Locale.ROOT);

        boolean isHiringPost = false;
        for (String keyword : HIRING_KEYWORDS)
        {
            if (combined.contains(keyword))
            {
                isHiringPost = true;
                break;
            }
        }

        if (!isHiringPost)
        {
            return null;
        }

        String link = entry.getLink();
        if (link == null || link.isBlank())
        {
            return null;
        }

        JobListing job = new JobListing();
        job.setJobTitle(truncate(title.isBlank() ? (blogName + " — hiring update") : title, 200));
        job.setCompanyName(truncate(blogName, 200));
        job.setSourceUrl(truncate(link, 500));
        job.setSource(source);
        job.setLocation("See post");
        job.setIsRemote(false);
        job.setDescription(truncate(stripHtml(summary), 2000));
        job.setListingType(JobListing.ListingType.full_time);
        job.setStatus(JobListing.Status.ACTIVE);

        if (entry.getPublishedDate() != null)
        {
            job.setPostedAt(LocalDateTime.ofInstant(
                    entry.getPublishedDate().toInstant(), ZoneId.systemDefault()));
        }

        return job;
    }

    private String stripHtml(String html)
    {
        if (html == null) return "";
        return html.replaceAll("<[^>]+>", " ").replaceAll("\\s+", " ").trim();
    }

    private String truncate(String s, int max)
    {
        if (s == null) return null;
        return s.length() > max ? s.substring(0, max) : s;
    }
}
